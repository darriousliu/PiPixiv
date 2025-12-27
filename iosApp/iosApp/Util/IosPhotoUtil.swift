import Foundation
import ComposeApp
import Photos

class IosPhotoUtil: PhotoUtil {
    static let shared = IosPhotoUtil()
    let albumName = "PiPixiv"

    func saveToAlbum(fileUri: URL, callback: any KotlinSuspendFunction1) async throws {
        if !(fileUri.isFileURL && FileManager.default.fileExists(atPath: fileUri.path)) {
            try await callback.invoke(p1: nil)
            return
        }
        // 2) 权限
        let auth = await PHPhotoLibrary.requestAuthorization(for: .readWrite)
        guard auth == .authorized || auth == .limited else {
            // limited 对“写入”是允许的，但如果你的业务需要读回相册内容，要额外处理。
            try await callback.invoke(p1: nil)
            return
        }
        // 3) 执行保存事务：创建 Asset + 创建/获取 Album + 加入 Album
        do {
            if let albumId = try await getOrCreateAlbumLocalIdentifier(named: self.albumName) {
                let localId = try await performSave(fileURL: fileUri, albumLocalIdentifier: albumId)
                try await callback.invoke(p1: localId)
            } else {
                try await callback.invoke(p1: nil)
            }
            return
        } catch {
            try await callback.invoke(p1: nil)
            return
        }
    }

    private func getOrCreateAlbumLocalIdentifier(named name: String) async throws -> String? {
        if let album = fetchAlbum(named: name) {
            return album.localIdentifier
        }

        return try await withCheckedThrowingContinuation { continuation in
            var createdAlbumId: String?

            PHPhotoLibrary.shared().performChanges {
                let request = PHAssetCollectionChangeRequest.creationRequestForAssetCollection(withTitle: name)
                createdAlbumId = request.placeholderForCreatedAssetCollection.localIdentifier
            } completionHandler: { success, error in
                if let error {
                    continuation.resume(returning: nil)
                    return
                }
                guard success, let id = createdAlbumId else {
                    continuation.resume(returning: nil)
                    return
                }
                continuation.resume(returning: id)
            }
        }
    }

    private func performSave(fileURL: URL, albumLocalIdentifier: String) async throws -> String? {
        try await withCheckedThrowingContinuation { continuation in
            var createdAssetLocalIdentifier: String?

            PHPhotoLibrary.shared().performChanges {
                // 1) 获取相册
                let albumFetch = PHAssetCollection.fetchAssetCollections(
                    withLocalIdentifiers: [albumLocalIdentifier],
                    options: nil
                )
                guard let album = albumFetch.firstObject else {
                    return
                }
                // 2) 创建资产（图片）
                let assetRequest = PHAssetCreationRequest.creationRequestForAssetFromImage(atFileURL: fileURL)
                guard let placeholder = assetRequest?.placeholderForCreatedAsset else {
                    // performChanges block 内不能 throw，这里用外层变量 + completion 处理
                    return
                }
                createdAssetLocalIdentifier = placeholder.localIdentifier

                // 3) 把新建的 asset 加入相册
                let addRequest = PHAssetCollectionChangeRequest(for: album)
                addRequest?.addAssets([placeholder] as NSArray)

            } completionHandler: { success, error in
                if let error {
                    continuation.resume(throwing: error)
                    return
                }
                guard success else {
                    continuation.resume(returning: nil)
                    return
                }
                guard let localId = createdAssetLocalIdentifier else {
                    continuation.resume(returning: nil)
                    return
                }
                continuation.resume(returning: localId)
            }
        }
    }

    private func fetchAlbum(named name: String) -> PHAssetCollection? {
        let options = PHFetchOptions()
        options.predicate = NSPredicate(format: "title = %@", name)

        let result = PHAssetCollection.fetchAssetCollections(with: .album, subtype: .any, options: options)
        return result.firstObject
    }
}

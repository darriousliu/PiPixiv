import Foundation
import ExportedKotlinPackages
import KotlinRuntime
import KotlinRuntimeSupport
import KotlinStdlib
import PiPixivPlatform
import ZIPFoundation

class IosZipUtil: util.ZipUtilBridge {
    static let shared = IosZipUtil()

    override func compress(sourcePath: String, destinationPath: String) -> Bool {
        let sourceURL = getValidURL(path: sourcePath)
        let destinationURL = getValidURL(path: destinationPath)
        do {
            try FileManager.default.zipItem(at: sourceURL, to: destinationURL)
            return true
        } catch {
            print("compress error: \(error)")
            return false
        }
    }

    override func getZipEntryContent(
        zipFilePath: String,
        entryName: String
    ) -> ExportedKotlinPackages.kotlin.ByteArray? {
        let sourceURL = getValidURL(path: zipFilePath)
        guard let archive = try? Archive(url: sourceURL, accessMode: .read),
              let entry = archive[entryName]
        else {
            return nil
        }

        var data = Data()
        do {
            _ = try archive.extract(entry) {
                data.append($0)
            }
        } catch {
            print("extract error: \(error)")
            return nil
        }

        return util.createByteArray(size: Int32(data.count)) { index in
            Int8(bitPattern: data[Int(index)])
        }
    }

    override func getZipEntryList(zipFilePath: String) -> [ExportedKotlinPackages.kotlin.Pair] {
        let sourceURL = getValidURL(path: zipFilePath)
        guard let archive = try? Archive(url: sourceURL, accessMode: .read) else {
            return []
        }

        var list: [ExportedKotlinPackages.kotlin.Pair] = []
        for entry in archive {
            let pair = ExportedKotlinPackages.kotlin.Pair(
                first: entry.path,
                second: entry.type == .directory
            )
            list.append(pair)
        }
        return list
    }

    override func unzip(sourcePath: String, destinationPath: String) -> Bool {
        let sourceURL = getValidURL(path: sourcePath)
        let destinationURL = getValidURL(path: destinationPath)
        do {
            try FileManager.default.createDirectory(at: destinationURL, withIntermediateDirectories: true, attributes: nil)
            try FileManager.default.unzipItem(at: sourceURL, to: destinationURL)
            return true
        } catch {
            print("unzip error: \(error)")
            return false
        }
    }

    private func getValidURL(path: String) -> URL {
        return if path.hasPrefix("file://") {
            URL(string: path)!
        } else {
            URL(fileURLWithPath: path)
        }
    }
}

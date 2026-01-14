import Foundation
import ComposeApp
import ZIPFoundation

class IosZipUtil: NSObject, ZipUtil {
    static let shared = IosZipUtil()

    func compress(sourcePath: String, destinationPath: String) -> Bool {
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

    func getZipEntryContent(zipFilePath: String, entryName: String) -> KotlinByteArray? {
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

        let byteArray = KotlinByteArray(size: Int32(data.count))
        for (index, byte) in data.enumerated() {
            byteArray.set(index: Int32(index), value: Int8(bitPattern: byte))
        }
        return byteArray
    }

    func getZipEntryList(zipFilePath: String) -> [KotlinPair<NSString, KotlinBoolean>] {
        let sourceURL = getValidURL(path: zipFilePath)
        guard let archive = try? Archive(url: sourceURL, accessMode: .read) else {
            return []
        }

        var list: [KotlinPair<NSString, KotlinBoolean>] = []
        for entry in archive {
            let path = entry.path as NSString
            let isDir = entry.type == .directory
            let pair = KotlinPair(first: path, second: KotlinBoolean(bool: isDir))
            list.append(pair)
        }
        return list
    }

    func unzip(sourcePath: String, destinationPath: String) -> Bool {
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

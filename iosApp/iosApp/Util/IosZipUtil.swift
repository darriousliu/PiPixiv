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
            // Use the default .none for speed because ZIPFoundation 0.9.20 has no compression level setting.
            try FileManager.default.zipItem(at: sourceURL, to: destinationURL)
            return true
        } catch {
            print("compress error: \(error)")
            return false
        }
    }

    override func readZipEntryContent(
        zipFilePath: String,
        entryName: String,
        maxBytes: Int32
    ) -> ExportedKotlinPackages.kotlin.ByteArray? {
        let sourceURL = getValidURL(path: zipFilePath)
        guard let archive = try? Archive(url: sourceURL, accessMode: .read, pathEncoding: nil),
              let entry = archive[entryName], entry.type == .file,
              maxBytes >= 0, entry.uncompressedSize <= UInt64(maxBytes)
        else {
            return nil
        }

        var data = Data()
        do {
            try readEntry(archive, entry: entry) { chunk in
                data.append(chunk)
            }
        } catch {
            print("Unable to read ZIP entry: \(type(of: error)), code \((error as NSError).code)")
            return nil
        }

        return data.withUnsafeBytes { (bytes: UnsafeRawBufferPointer) in
            util.createByteArray(size: Int32(bytes.count)) { index in
                Int8(bitPattern: bytes[Int(index)])
            }
        }
    }

    override func getZipEntryMetadata(zipFilePath: String) -> [util.ZipEntryMetadata]? {
        let sourceURL = getValidURL(path: zipFilePath)
        guard let archive = try? Archive(url: sourceURL, accessMode: .read, pathEncoding: nil) else {
            return nil
        }
        var list: [util.ZipEntryMetadata] = []
        for entry in archive {
            guard let size = Int64(exactly: entry.uncompressedSize) else { return nil }
            list.append(util.ZipEntryMetadata(name: entry.path, size: size, isDirectory: entry.type != .file))
        }
        return list
    }

    override func getZipEntryList(zipFilePath: String) -> [ExportedKotlinPackages.kotlin.Pair] {
        let sourceURL = getValidURL(path: zipFilePath)
        guard let archive = try? Archive(url: sourceURL, accessMode: .read, pathEncoding: nil) else {
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

    override func extractZipEntry(zipFilePath: String, entryName: String, outputPath: String, maxBytes: Int64) -> Bool {
        let sourceURL = getValidURL(path: zipFilePath)
        guard let archive = try? Archive(url: sourceURL, accessMode: .read, pathEncoding: nil),
              let entry = archive[entryName], entry.type == .file,
              maxBytes >= 0, entry.uncompressedSize <= UInt64(maxBytes)
        else {
            return false
        }

        let outputURL = getValidURL(path: outputPath)
        let fileManager = FileManager.default
        guard !fileManager.fileExists(atPath: outputURL.path),
              fileManager.createFile(atPath: outputURL.path, contents: nil) else {
            return false
        }
        do {
            let output = try FileHandle(forWritingTo: outputURL)
            defer { try? output.close() }
            try readEntry(archive, entry: entry) { chunk in
                try output.write(contentsOf: chunk)
            }
            return true
        } catch {
            try? fileManager.removeItem(at: outputURL)
            print("Unable to extract ZIP entry")
            return false
        }
    }

    private func readEntry(_ archive: Archive, entry: Entry, write: (Data) throws -> Void) throws {
        var remaining = entry.uncompressedSize
        let crc = try archive.extract(entry) { chunk in
            let count = UInt64(chunk.count)
            guard count <= remaining else { throw CocoaError(.fileReadCorruptFile) }
            try write(chunk)
            remaining -= count
        }
        guard remaining == 0, crc == entry.checksum else { throw CocoaError(.fileReadCorruptFile) }
    }

    override func unzip(sourcePath: String, destinationPath: String) -> Bool {
        let sourceURL = getValidURL(path: sourcePath)
        let destinationURL = getValidURL(path: destinationPath)
        do {
            try FileManager.default.createDirectory(at: destinationURL, withIntermediateDirectories: true, attributes: nil)
            try FileManager.default.unzipItem(at: sourceURL, to: destinationURL)
            return true
        } catch {
            print("Unable to extract ZIP archive: \(type(of: error)), code \((error as NSError).code)")
            return false
        }
    }

    private func getValidURL(path: String) -> URL {
        return if path.hasPrefix("file://") {
            URL(string: path)!
        } else {
            URL(filePath: path, directoryHint: .checkFileSystem)
        }
    }
}

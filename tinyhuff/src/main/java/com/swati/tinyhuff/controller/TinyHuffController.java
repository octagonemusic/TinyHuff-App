package com.swati.tinyhuff.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.swati.tinyhuff.services.CompressorService;
import com.swati.tinyhuff.services.DecompressorService;

import java.io.FileInputStream;

@RestController
public class TinyHuffController {
    @Autowired
    private CompressorService compressorService;

    @Autowired
    private DecompressorService decompressorService;

    @PostMapping("/compress")
    public ResponseEntity<StreamingResponseBody> compress(@RequestParam("file") MultipartFile multipartFile)
            throws IOException {
        File inputFile = convert(multipartFile);
        File[] outputFiles = compressorService.compressor(inputFile);

        // Create a ZIP file
        File zipFile = new File("compressed_files.zip");
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile))) {
            for (File outputFile : outputFiles) {
                try (FileInputStream fis = new FileInputStream(outputFile)) {
                    ZipEntry zipEntry = new ZipEntry(outputFile.getName());
                    zipEntry.setSize(outputFile.length());
                    zipOut.putNextEntry(zipEntry);
                    StreamUtils.copy(fis, zipOut);
                    zipOut.closeEntry();
                }
                outputFile.delete(); // Delete the output file after it has been added to the ZIP file
            }
        }

        inputFile.delete(); // Delete the input file after it is no longer needed

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + zipFile.getName());

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(outputStream -> {
                    try (FileInputStream fis = new FileInputStream(zipFile)) {
                        StreamUtils.copy(fis, outputStream);
                    } finally {
                        zipFile.delete(); // Delete the ZIP file after it has been sent in the response
                    }
                });
    }

    private File convert(MultipartFile file) throws IOException {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File convFile = new File(tempDir, file.getOriginalFilename());
        file.transferTo(convFile);
        return convFile;
    }

    @PostMapping("/decompress")
    public ResponseEntity<StreamingResponseBody> decompress(@RequestParam("file") MultipartFile file)
            throws IOException {
        File zipFile = convert(file);

        // Unzip the file
        List<File> unzippedFiles = unzip(zipFile);

        // Sort the files
        File huffFile = null;
        File serFile = null;
        File txtFile = null;
        for (File unzippedFile : unzippedFiles) {
            if (unzippedFile.getName().endsWith(".huff")) {
                huffFile = unzippedFile;
            } else if (unzippedFile.getName().endsWith(".ser")) {
                serFile = unzippedFile;
            } else if (unzippedFile.getName().endsWith(".txt")) {
                txtFile = unzippedFile;
            }
        }

        if (huffFile == null || serFile == null || txtFile == null) {
            String errorMessage = "The zip file must contain a .huff file, a .ser file, and a .txt file.";
            return ResponseEntity.badRequest()
                    .body(responseStream -> responseStream.write(errorMessage.getBytes()));
        }

        // Pass the files to the decompressor method and get the output file
        File outputFile = decompressorService.decompressor(huffFile, serFile, txtFile);

        // Delete the unzipped files after they are no longer needed
        for (File unzippedFile : unzippedFiles) {
            unzippedFile.delete();
        }

        // Delete the zip file after it is no longer needed
        zipFile.delete();

        // Prepare the headers for the response
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + outputFile.getName());

        // Return the file as a response
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(outputStream -> {
                    try (FileInputStream fis = new FileInputStream(outputFile)) {
                        StreamUtils.copy(fis, outputStream);
                    } finally {
                        outputFile.delete(); // Delete the output file after it has been sent in the response
                    }
                });
    }

    private List<File> unzip(File zipFile) throws IOException {
        List<File> unzippedFiles = new ArrayList<>();

        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File unzipDir = new File(tempDir, UUID.randomUUID().toString());
        unzipDir.mkdir();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = newFile(unzipDir, zipEntry);
                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
                unzippedFiles.add(newFile);
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        }

        return unzippedFiles;
    }

    private File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }
}

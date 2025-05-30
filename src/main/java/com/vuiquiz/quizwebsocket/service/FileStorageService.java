package com.vuiquiz.quizwebsocket.service;

import com.vuiquiz.quizwebsocket.exception.FileStorageException;
import com.vuiquiz.quizwebsocket.exception.MyFileNotFoundException;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface FileStorageService {
    /**
     * Stores a file to the configured upload directory.
     *
     * @param file The file to store.
     * @return The unique filename (including any sub-path from the upload root) under which the file is stored.
     * @throws FileStorageException if an error occurs during file storage.
     */
    String storeFile(MultipartFile file);

    /**
     * Loads a file as a resource.
     *
     * @param filename The name of the file to load (relative to the upload directory).
     * @return The loaded file as a Resource.
     * @throws MyFileNotFoundException if the file is not found.
     */
    Resource loadFileAsResource(String filename);

    /**
     * Resolves the absolute path to a stored file.
     *
     * @param filename The name of the file (relative to the upload directory).
     * @return The absolute Path object.
     */
    Path resolveFilePath(String filename);

    /**
     * Deletes a file from the storage.
     *
     * @param storedFilename The unique filename (including any sub-path from the upload root) of the file to delete.
     * @throws MyFileNotFoundException if the file does not exist.
     * @throws FileStorageException if an error occurs during deletion.
     */
    void deleteFile(String storedFilename);
}
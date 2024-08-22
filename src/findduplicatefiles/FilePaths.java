/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package findduplicatefiles;

/**
 *
 * @author heather
 */
import java.nio.file.Path;
import java.nio.file.FileSystems;

public class FilePaths {
    private Path parentDir;
    private Path duplicatePath;
    private Path originalPath;

    public FilePaths(Path duplicatePath, Path originalPath) {
        this.duplicatePath = duplicatePath;
        this.originalPath  = originalPath;
    }

    public Path getDuplicatePath() {
        return duplicatePath;
    }

    public Path getOriginalPath() {
        return originalPath;
    }
    
    public static Path getParentDir(String dirString) {
        return FileSystems.getDefault().getPath(dirString);
    }

    @Override
    public String toString() {
        return String.format("(duplicate: %s, original: %s)", duplicatePath, originalPath);
    }
}
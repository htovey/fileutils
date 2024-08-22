/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package findduplicatefiles;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Deque;
import java.util.ArrayDeque;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.MessageDigest;
import java.security.DigestInputStream;
import java.security.NoSuchAlgorithmException;
import java.math.BigInteger;


/**
 *
 * @author heather
 */
public class FindDuplicateFiles {
    private static int SAMPLE_SIZE = 10;
    public static void main(String[] args) {
       //input: time/date range
       //output: list of dupe files in /heather/TestDocuments
       List<FilePaths> filePathList = getDuplicateFileListUsingSampleBytes();
       filePathList.forEach((filePath) -> {
           System.out.println(filePath.toString());
       });
    }
    
    public static List<FilePaths> getDuplicateFileListUsingSpaceHogFileContents(String [] timeStrings) {
        List<FilePaths> duplicates = new ArrayList<>();
        //check to see if files are duplicates of each other...how?
        //build a stack to search file list then
        //track using outside scope using HashMap - store string of file contents
        //as key, FileInfo as value
        Map<String,FileInfo> duplicateChecker = new HashMap<>();
        //create stack from fileList
        /******************************************************************
         * DEQUE CLASS - extends QUEUE -  both are Collections Interfaces
         * Deque is implemented by ArrayDeque and LinkedList.  Used to create
         * a 'Double Ended Queue' or stack to which items can be added/removed
         * at head and at end.
         * 
         * pop() - get first element   POP = GET
         * push() - put element at head of stack  PUSH = PUT
         ******************************************************************/
        Deque<Path> stack = new ArrayDeque<>();
        //stack.push(directory);
        while(!stack.isEmpty()) {
            //take first element of stack - top element, convert to File
           Path currentPath = stack.pop();
           File currentFile = currentPath.toFile();
           //check if directory then search, push files to stack
           if (currentFile.isDirectory()) {
               for(File file : currentFile.listFiles()) {
                   stack.push(file.toPath());
               }
           } else {
              //if it's a file, read the file and get string of contents and lastMod time
              String fileContents = null;
              try {
                  //get byte array of contents and create String
                  byte [] contentByteArray = Files.readAllBytes(currentPath);
                  fileContents = new String(contentByteArray, "UTF-8");
              } catch(IOException ioex) {
                  System.out.println(ioex);
                  //skip file and continue while loop
                  continue;
              }
              //if no error, get mod time of file
              long currentModTime = currentFile.lastModified();   
              //check Map for existence of contents - if dupe, create entry for 
              //duplicates list, otherwise, create map entry
              if (!duplicateChecker.containsKey(fileContents)) {
                  //create FileInfo and add to map with contents as key
                  FileInfo fileInfo = new FileInfo(currentModTime, currentPath);
                  duplicateChecker.put(fileContents, fileInfo);
              } else {
                  //fileContents are equal, so check the lastModTime
                  FileInfo existingFileInfo = duplicateChecker.get(fileContents);
                  if (currentModTime > existingFileInfo.timeLastModified) {
                  // add filepaths entry with path from map, and path from current
                    Path duplicatePath = existingFileInfo.path;
                    duplicates.add(new FilePaths(currentPath, duplicatePath));
                    
                    //in addition to adding dupe to list, add to map
                    duplicateChecker.put(fileContents, new FileInfo(currentModTime, currentPath));
                  }
              }
           }
        }
        
        return duplicates;
    }
    
    public static List<FilePaths> getDuplicateFileListUsingFileSize(String [] args) {
        List<FilePaths> duplicates = new ArrayList<>();
        //like previous method, we can use a HashMap and Deque to track our file objects
        //the Deque offers ordered access to the file tree, whereas a list would require 
        //using an index, and is in no particular order??
        //however, instead of using the entire contents of a file for comparison,  lets
        //use file size as the key to store in map and to compare for duplicates
        /**
         * DFS - depth first search-  search each 'branch' of directory tree using Deque
         * push then pop....debug to see how this plays out  
         * PUSH = PUT and POP = GET & Remove
         * So you POP the directory, then PUSH the files in it to the stack
         */
        Map<Long, FileInfo> duplicateChecker = new HashMap<>();
        Deque<Path> stack = new ArrayDeque<>();
        //1) put main directory on stack (push)
        //stack.push(directory);
        //2) 
        //iterate over stack to perform DFS.. start with pop for dir then push for files
        while(!stack.isEmpty()) {
            //take first 'plate' off the stack
            Path currentPath = stack.pop();
            File currentFile = currentPath.toFile();
            //if file is Directory, iterate over files and put them in stack
            if (currentFile.isDirectory()) {
                for (File file : currentFile.listFiles()) {
                    stack.push(file.toPath());
                }
            } else {
                long fileSize = currentFile.length();
                long currentModTime = currentFile.lastModified();
                //otherwise, check file to see if it's a duplicate based on size
                if (duplicateChecker.containsKey(fileSize)) {
                    FileInfo existingFileInfo = duplicateChecker.get(fileSize);
                  if (currentModTime > existingFileInfo.timeLastModified) {
                  // add filepaths entry with path from map, and path from current
                    Path duplicatePath = existingFileInfo.path;
                    duplicates.add(new FilePaths(currentPath, duplicatePath));
                    
                    //in addition to adding dupe to list, add to map, overwriting previous
                    //map entry with new path
                    duplicateChecker.put(fileSize, new FileInfo(currentModTime, currentPath));
                  } else {
                      FileInfo newFileInfo = new FileInfo(currentFile.lastModified(), currentPath);
                      duplicateChecker.put(fileSize, newFileInfo);
                  }
                }
            }
            
        }
        return duplicates;
    }
    
     public static List<FilePaths> getDuplicateFileListUsingSampleBytes() {
        Path directory = FileSystems.getDefault().getPath("/Users/heather/TestFiles");
        List<FilePaths> duplicates = new ArrayList<>();
        //like previous method, we can use a HashMap and Deque to track our file objects
        //the Deque offers ordered access to the file tree, whereas a list would require 
        //using an index, and is in no particular order??
        //however, instead of using the entire contents of a file for comparison,  lets
        //use sample bytes from specific indices as the key to store in map and to compare for duplicates
        /**
         * DFS - depth first search-  search each 'branch' of directory tree using Deque
         * push then pop....debug to see how this plays out  
         * PUSH = PUT and POP = GET & Remove
         * So you POP the directory, then PUSH the files in it to the stack
         */
        Map<String, FileInfo> duplicateChecker = new HashMap<>();
        Deque<Path> stack = new ArrayDeque<>();
        //1) put main directory on stack (push)
        stack.push(directory);
        //2) 
        //iterate over stack to perform DFS.. start with pop for dir then push for files
        while(!stack.isEmpty()) {
            //take first 'plate' off the stack
            Path currentPath = stack.pop();
            File currentFile = currentPath.toFile();
            //if file is Directory, iterate over files and put them in stack
            if (currentFile.isDirectory()) {
                for (File file : currentFile.listFiles()) {
                    stack.push(file.toPath());
                }
            } else {
                String fileKey = null;
                try {
                    fileKey = buildSampleHash(currentFile);
                } catch (IOException | NoSuchAlgorithmException ex) {
                    Logger.getLogger(FindDuplicateFiles.class.getName()).log(Level.SEVERE, null, ex);
                } 
                long currentModTime = currentFile.lastModified();
                //otherwise, check file to see if it's a duplicate based on size
                if (duplicateChecker.containsKey(fileKey)) {
                    //map contains duplicate- check to see which is original and which is copy
                    FileInfo existingFileInfo = duplicateChecker.get(fileKey);
                  if (currentModTime > existingFileInfo.timeLastModified) {
                  //current file is the duplicate, so create list entry with current path and existing
                    Path existingFilePath = existingFileInfo.path;
                    duplicates.add(new FilePaths(currentPath, existingFilePath));
                    
                  } else {
                      //existing file is duplicate, so create list entry with exixting then current
                      duplicates.add(new FilePaths(existingFileInfo.path, currentPath));
                      //update map with new file
                      duplicateChecker.put(fileKey, new FileInfo(currentModTime, currentPath));
                  }
                } else {
                    duplicateChecker.put(fileKey, new FileInfo(currentModTime, currentPath));
                }
            }
            
        }
        return duplicates;
    }
     
    public String buildSampleKey(File file) {
        String a = null;
        String b = null;
        String c = null;
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            raf.seek(0);
            a = String.valueOf(raf.readByte());
            raf.seek(file.length() / 2);
            b = String.valueOf(raf.readChar());
            raf.seek(file.length() - 1);
            c = String.valueOf(raf.readChar());
        } catch(FileNotFoundException fnfex) {
            System.out.println(fnfex);
        } catch (IOException ex) {
            Logger.getLogger(FindDuplicateFiles.class.getName()).log(Level.SEVERE, null, ex);
        }
        return a+b+c;
    } 
    
    private static String buildSampleHash(File file) throws IOException, NoSuchAlgorithmException {
        String fileHash = "";
        final long length = file.length();
        
         try(InputStream inputStream = new FileInputStream(file)) {
             
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            //interview cake solution uses IO stream and Digest classes....
            /**1) Hashes use HASHING ALGORITHMS:
            *   SHA-2 (256) Secure Hash Algorithm
            *   MD-5  Message Digest (512)
            *   MessageDigest class - takes algorithm as constructor param, either SHA-X or MD-X
            *   DigestInputStream is an input stream with a MessageDigest member upon which it performs computation
            **/
       
            DigestInputStream dis = new DigestInputStream(inputStream, md);
            
                // if the file is too short to take 3 samples, hash the entire file
            if (length < SAMPLE_SIZE * 3) {
                byte[] bytes = new byte[(int) length];
                dis.read(bytes);
            } else {
                byte [] bytes = new byte[(int)length];
                //calculate number of bytes between samples
                long sampleInterval = (length - SAMPLE_SIZE * 3) / 2;
                //use int loop to iterate over stream:
                int numberOfSamples = 3;
                for (int i=0; i < numberOfSamples; i++) {
                    //read from stream into byte array that was
                    //pre fabricated to be size of 3 samples combined
                    //starting at offset i TIMES SAMPLE_SIZE, to end of SAMPLE_SIZE length
                    //so if sample size is 30:
                    //i = 0  =>  read sample into byte array starting at 0 and going to 30, then skip
                    //sampleInterval, 
                    //i = 1 => 1 TIMES 30 = 30 - read into byte array starting at 30 and going 60 then skip 
                    //sampleInterval,
                    //i = 2 => read into byte array starting at 60 then go to 90
                    // read() method calls MessageDigest.update() with byte [] and offset/length, which will then
                    // be updated for eventual computation of entire array via digest()
                    dis.read(bytes, i * SAMPLE_SIZE, SAMPLE_SIZE);
                    dis.skip(sampleInterval);
                }
            }
                //now that byte [] of samples has been built, use MessageDigest to return
                //it in a String using BigInteger Constructor which takes an int for sign bit (1 for positive, 0, -1 for negative)
                //plus the byte array
            fileHash = new BigInteger(1, md.digest()).toString(16);
                
            
            
        } 
        return fileHash;
    }
    
    //instead of finding files within range, mark each file in fileinfo object
    //replace parseTime method with helper class that stores lastModified time with path
    private static class FileInfo {
        long timeLastModified;
        Path path;
        
        FileInfo(long timeLastModified, Path path) {
            this.timeLastModified = timeLastModified;
            this.path = path;
        }
    }
    
}

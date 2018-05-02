package mappyss.maphive.io.mappyss;

import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by oldwang on 2018/4/23.
 *
 */

public class FileUtils {


    public static List<File> filterPhotoFile(File rootFile, final String buildingId) {
        List<File> fileList = new ArrayList<>();
        File[] files = rootFile.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return (file.isDirectory() && file.getName().contains(buildingId));
            }
        });
        if (files.length > 0) {
            for (File file : files) {
                File[] photoFiles = file.listFiles();
                for (File photoFile : photoFiles) {
                    if (photoFile.isFile()) {
                        fileList.add(photoFile);
                    } else if (photoFile.isDirectory()) {
                        fileList.addAll(Arrays.asList(photoFile.listFiles()));
                    }
                }
            }
        }
        File[] folderFiles = rootFile.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
        for (File folderFile : folderFiles) {
            List<File> resultFiles = filterPhotoFile(folderFile, buildingId);
            if (!resultFiles.isEmpty()) {
                fileList.addAll(resultFiles);
            }
        }
        return fileList;
    }
}


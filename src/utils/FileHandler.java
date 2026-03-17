package utils;

import java.io.FileWriter;
import java.io.IOException;

public class FileHandler {

    public static void writeToFile(String data) {

        try {
            FileWriter writer = new FileWriter("data.txt", true);
            writer.write(data + "\n");
            writer.close();
        } catch(IOException e) {
            System.out.println("Error writing file");
        }

    }

}
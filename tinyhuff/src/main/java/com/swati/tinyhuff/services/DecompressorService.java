package com.swati.tinyhuff.services;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.File;

@Component
@Scope("prototype")
public class DecompressorService {
    private ArrayList<Boolean> readBits(int bitCount, FileInputStream in) throws IOException {
        ArrayList<Boolean> encoded = new ArrayList<>();
        int byteValue = 0;
        int bitIndex = 0;
        while (bitCount > 0) {
            if (bitIndex == 0) {
                byteValue = in.read();
                if (byteValue == -1) {
                    break;
                }
            }
            boolean bit = ((byteValue >> (7 - bitIndex)) & 1) == 1;
            encoded.add(bit);
            bitIndex = (bitIndex + 1) % 8;
            bitCount--;
        }
        in.close();
        return encoded;
    }

    private File decoder(Node head, int length, File compressedFile) {
        StringBuilder temp = new StringBuilder();
        File outputFile = new File("textOutput.txt");

        try {
            ArrayList<Boolean> encoded = null;
            encoded = readBits(length, new FileInputStream(compressedFile));
            Node current = head;
            for (int i = 0; i < encoded.size(); i++) {
                if (current == null) {
                    System.err.println("Error: current node is null");
                    return null;
                }
                current = encoded.get(i) ? current.lptr : current.rptr;
                if (current.lptr == null && current.rptr == null) {
                    temp.append(current.c);
                    current = head;
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading bits: " + e.getMessage());
            return null;
        }

        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(temp.toString());
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }

        return outputFile;
    }

    private static Node deserialize(File file) {
        Node root = null;
        try (FileInputStream fileIn = new FileInputStream(file);
                ObjectInputStream in = new ObjectInputStream(fileIn)) {
            root = (Node) in.readObject();
            System.out.println("Tree deserialized from " + file.getName());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return root;
    }

    public File decompressor(File compressed, File tree, File bitCountFile) {

        int bitcount = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(bitCountFile))) {
            bitcount = Integer.parseInt(br.readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Encoded size: " + bitcount);

        Node root = deserialize(tree);
        decoder(root, bitcount, compressed);
        return new File("textOutput.txt");
    }
}

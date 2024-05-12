package com.swati.tinyhuff.services;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.ObjectOutputStream;
import java.io.FileWriter;

@Component
@Scope("prototype")
public class CompressorService {

    private int[][] frequency(String s) {
        int[][] charFrequency = new int[256][2];
        for (int i = 0; i < 256; i++)
            charFrequency[i][0] = i;

        for (int j = 0; j < s.length(); j++) {
            char c = s.charAt(j);
            charFrequency[(int) c][1]++;
        }
        return charFrequency;
    }

    private Node[] least2nodes(List<Node> list) {
        Node[] result = new Node[2];
        int min1 = Integer.MAX_VALUE;
        int min2 = Integer.MAX_VALUE;

        for (Node node : list) {
            if (node.data < min1) {
                min2 = min1;
                min1 = node.data;
                result[1] = result[0];
                result[0] = node;
            } else if (node.data < min2) {
                min2 = node.data;
                result[1] = node;
            }
        }
        return result;
    }

    private Node createtree(List<Node> list) {
        char c = '\u0000';

        while (list.get(0) != list.get(list.size() - 1)) {
            Node[] least2 = least2nodes(list);
            Node a = least2[0];
            Node b = least2[1];
            Node n = new Node(a.data + b.data, c, a, b);
            list.remove(least2[0]);
            list.remove(least2[1]);
            list.add(n);
        }
        return list.get(0);
    }

    private boolean isLeafNode(Node n, List<Node> list) {
        return list.contains(n);
    }

    private void createCodes(Node head, List<Node> list, String s, HashMap<Character, String> hashMap) {
        if (head == null) {
            return;
        }
        if (isLeafNode(head, list)) {
            hashMap.put(head.c, s);
            s = "";
        }
        createCodes(head.lptr, list, s + "1", hashMap);
        createCodes(head.rptr, list, s + "0", hashMap);
    }

    private File[] encoder(String s, HashMap<Character, String> hashMap, String fileName) {
        String temp = "";
        String temp2 = "";
        ArrayList<Boolean> encoded = new ArrayList<>();
        for (int i = 0; i < s.length(); i++) {
            temp2 = hashMap.get(s.charAt(i));
            temp = temp + temp2;
        }
        for (int i = 0; i < temp.length(); i++) {
            if (temp.charAt(i) == '1') {
                encoded.add(true);
            } else {
                encoded.add(false);
            }
        }
        int bitCount = encoded.size();
        System.out.println("Encoded size: " + bitCount);

        File compressedFile = new File(fileName + "textCompressed.huff");
        File bitCountFile = new File(fileName + "textBitCount.txt");

        try (FileOutputStream out = new FileOutputStream(compressedFile);
                FileWriter out2 = new FileWriter(bitCountFile)) {
            writeBits(out, encoded);
            System.out.println(bitCount);
            out2.write(String.valueOf(bitCount));
        } catch (IOException e) {
            e.printStackTrace();
        }

        File[] files = { compressedFile, bitCountFile };
        return files;
    }

    private void writeBits(FileOutputStream out, ArrayList<Boolean> bits) throws IOException {
        int byteValue = 0;
        int bitCount = 0;
        for (int i = 0; i < bits.size(); i++) {
            byteValue = (byteValue << 1) | (bits.get(i) ? 1 : 0);
            bitCount++;

            if (bitCount == 8) {
                out.write(byteValue);
                byteValue = 0;
                bitCount = 0;
            }
        }
        if (bitCount > 0) {
            byteValue <<= (8 - bitCount);
            out.write(byteValue);
        }
        // return a file with content as bitcount
    }

    private File serialize(Node root, String filename) {
        File outputFile = new File(filename);
        try (FileOutputStream fileOut = new FileOutputStream(outputFile);
                ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            out.writeObject(root);
            System.out.println("Tree serialized and written to " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputFile;
    }

    public File[] compressor(File inputFile) throws IOException {
        String s = "";
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            StringBuilder sb = new StringBuilder();

            while ((s = br.readLine()) != null) {
                sb.append(s).append("\n");
            }
            s = sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String fileName = inputFile.getName();
        String file[] = fileName.split("\\.");
        fileName = file[0];
        int[][] freq = frequency(s);
        List<Node> list = new LinkedList<Node>();
        List<Node> list2 = new LinkedList<Node>();
        for (int i = 0; i < 256; i++) {
            if (freq[i][1] > 0) {
                Node n = new Node(freq[i][1], (char) freq[i][0], null, null);
                list.add(n);
                list2.add(n);
            }
        }
        Node head = createtree(list2);

        HashMap<Character, String> hashMap = new HashMap<>();
        createCodes(head, list, "", hashMap);
        File[] encoded = encoder(s, hashMap, fileName);
        File compressed = encoded[0];
        File bitCountFile = encoded[1];
        File tree = serialize(head, fileName + "tree.ser");
        File files[] = { compressed, tree, bitCountFile };
        return files;
    }

}
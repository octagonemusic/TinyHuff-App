package com.swati.tinyhuff.services;

import java.io.Serializable;

public class Node implements Serializable {
    public int data;
    public char c;
    public Node lptr, rptr;

    Node(int data, char c, Node lptr, Node rptr) {
        this.data = data;
        this.c = c;
        this.lptr = lptr;
        this.rptr = rptr;
    }
}

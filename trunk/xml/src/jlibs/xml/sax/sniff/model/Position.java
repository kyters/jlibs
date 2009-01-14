/**
 * JLibs: Common Utilities for Java
 * Copyright (C) 2009  Santhosh Kumar T
 * <p/>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */

package jlibs.xml.sax.sniff.model;

import org.jaxen.saxpath.Axis;

/**
 * @author Santhosh Kumar T
 */
public class Position extends Node{
    public int axis;
    public int pos;

    public Position(Node node, int axis, int pos){
        super(node);
        this.axis = axis;
        this.pos = pos;
    }

    @Override
    public boolean matchesElement(String uri, String name, int position){
        return true;
//        if(axis==Axis.CHILD)
//            return pos==position;
//        else
//            return true;
    }

    @Override
    public String toString(){
        return "["+pos+"]";
    }
}

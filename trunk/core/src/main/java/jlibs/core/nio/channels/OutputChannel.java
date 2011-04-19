/**
 * JLibs: Common Utilities for Java
 * Copyright (C) 2009  Santhosh Kumar T <santhosh.tekuri@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */

package jlibs.core.nio.channels;

import jlibs.core.nio.AttachmentSupport;
import jlibs.core.nio.ClientChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * @author Santhosh Kumar T
 */
public abstract class OutputChannel extends AttachmentSupport implements WritableByteChannel{
    protected final NIOSupport nioSupport;

    protected OutputChannel(NIOSupport nioSupport){
        this.nioSupport = nioSupport;
        nioSupport.attachHandler();
        nioSupport.setOutput(this);
    }

    public final ClientChannel client(){
        return nioSupport.client();
    }

    private boolean statusInterested;
    public final void addStatusInterest(){
        statusInterested = true;
    }

    public final void removeStatusInterest(){
        statusInterested = false;
    }

    private boolean writeInterested;
    public final void addWriteInterest(){
        if(status()!=Status.NEEDS_OUTPUT){
            if(handler!=null){
                handler.onWrite(this);
            }
        }else
            writeInterested = true;
    }

    protected abstract boolean activateInterest();

    public void removeWriteInterest() throws IOException{
        writeInterested = false;
        if(status()!=Status.NEEDS_OUTPUT)
            nioSupport.removeInterest();
    }

    protected OutputHandler handler;
    public void setHandler(OutputHandler handler){
        this.handler = handler;
    }

    @Override
    public final int write(ByteBuffer src) throws IOException{
        if(src.remaining()==0 || status()==Status.NEEDS_OUTPUT)
            return 0;
        try{
            int wrote = onWrite(src);
            if(wrote>0)
                onWrite();
            return wrote;
        }catch(IOException ex){
            onIOException();
            throw ex;
        }
    }

    protected abstract int onWrite(ByteBuffer src) throws IOException;
    protected void onIOException(){}

    private boolean closed;

    @Override
    public final boolean isOpen(){
        return !closed;
    }

    @Override
    public final void close() throws IOException{
        closed = true;
        writeInterested = false;
        doClose();
        onWrite();
    }

    protected void doClose() throws IOException{}

    protected final void onWrite() throws IOException{
        Status earlierStatus = status();
        if(earlierStatus==Status.NEEDS_OUTPUT)
            writePending();
        Status curStatus = status();
        if(curStatus==Status.NEEDS_OUTPUT)
            nioSupport.addInterest();
        else
            notifyCompleted(earlierStatus, curStatus);
    }

    protected abstract void writePending() throws IOException;
    public abstract Status status();
    protected void notifyCompleted(Status earlierStatus, Status curStatus){
        OutputChannel output = nioSupport.getOutput();
        if(output==this && !isOpen() && curStatus==Status.COMPLETED) // favor GC
            nioSupport.setOutput(null);
        if(output.statusInterested){
            output.statusInterested = false;
            if(earlierStatus!=curStatus && output.handler!=null)
                output.handler.onStatus(output);
        }
        if(output.writeInterested){
            output.writeInterested = false;
            if(output.handler!=null)
                output.handler.onWrite(output);
        }
    }

    @Override
    public String toString(){
        return getClass().getSimpleName()+'('+nioSupport.client()+')';
    }

    public enum Status{ NEEDS_INPUT, NEEDS_OUTPUT, COMPLETED }
}

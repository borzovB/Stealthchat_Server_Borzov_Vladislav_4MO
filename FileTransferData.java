package org.face_recognition;

///Класс для описания объекта данных

import java.io.Serializable;

class FileTransferData implements Serializable {
    private String senderName;
    private String recipientName;
    private byte[] fileChunk;
    private boolean isLastChunk;
    private String nameFile;

    public FileTransferData(String senderName, String recipientName, byte[] fileChunk, boolean isLastChunk, String nameFile) {
        this.senderName = senderName;
        this.recipientName = recipientName;
        this.fileChunk = fileChunk;
        this.isLastChunk = isLastChunk;
        this.nameFile = nameFile;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public byte[] getFileChunk() {
        return fileChunk;
    }
    public String getFileName() {
        return nameFile;
    }

    public boolean isLastChunk() {
        return isLastChunk;
    }
}

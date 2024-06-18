package dev.dagless.model.transfer;

import java.util.List;

public class FileTransfer {

    private Provider provider;
    private TransferType transferType;
    private List<File> files;

    public FileTransfer(Provider provider, TransferType transferType) {
        this.provider = provider;
        this.transferType = transferType;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public TransferType getTransferType() {
        return transferType;
    }

    public void setTransferType(TransferType transferType) {
        this.transferType = transferType;
    }

    public List<File> getFiles() {
        return files;
    }

    public void setFiles(List<File> files) {
        this.files = files;
    }

    @Override
    public String toString() {
        return "FileTransfer{" +
                "provider=" + provider +
                ", transferType=" + transferType +
                ", files=" + files +
                '}';
    }
}

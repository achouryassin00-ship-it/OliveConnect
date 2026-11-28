package com.oliveconnect.dto;

public class TourneeActionDTO {

    private Long version;

    public TourneeActionDTO() {
    }

    public TourneeActionDTO(Long version) {
        this.version = version;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}

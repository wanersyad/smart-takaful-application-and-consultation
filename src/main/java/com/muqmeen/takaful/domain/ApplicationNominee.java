package com.muqmeen.takaful.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "application_nominees")
public class ApplicationNominee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private ConsultationApplication application;

    @Column(length = 120)
    private String fullName;

    @Column(length = 40)
    private String icNumber;

    @Column(length = 80)
    private String relationship;

    @Column(length = 500)
    private String homeAddress;

    @Column(length = 120)
    private String occupation;

    @Column(length = 30)
    private String phoneNumber;

    public Long getId() { return id; }
    public ConsultationApplication getApplication() { return application; }
    public void setApplication(ConsultationApplication application) { this.application = application; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getIcNumber() { return icNumber; }
    public void setIcNumber(String icNumber) { this.icNumber = icNumber; }
    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }
    public String getHomeAddress() { return homeAddress; }
    public void setHomeAddress(String homeAddress) { this.homeAddress = homeAddress; }
    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}

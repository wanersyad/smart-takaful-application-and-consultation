package com.muqmeen.takaful.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * One completed Plan Finder questionnaire. Answers are stored as coarse option codes (age band,
 * budget band, goals...) rather than free text, so the AI only ever sees non-identifying data.
 * The optional health answer is stored for the agent but is never sent to the AI.
 * Links to the customer, application and consultation are filled in later when the visitor
 * acts on the result; they are cleared automatically if those records are deleted.
 */
@Entity
@Table(name = "plan_finder_responses")
public class PlanFinderResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unguessable public id used in the result page URL. */
    @Column(nullable = false, unique = true, length = 36)
    private String token;

    @Column(nullable = false, length = 2)
    private String language;

    @Column(nullable = false, length = 20)
    private String ageBand;

    @Column(nullable = false, length = 20)
    private String maritalStatus;

    @Column(nullable = false, length = 10)
    private String childrenBand;

    /** Comma-separated goal codes. */
    @Column(nullable = false, length = 300)
    private String goals;

    @Column(nullable = false, length = 20)
    private String existingCoverage;

    @Column(nullable = false, length = 20)
    private String budgetBand;

    @Column(nullable = false, length = 20)
    private String occupationClass;

    /** Agent-only. Never included in the AI prompt. */
    @Column(length = 20)
    private String healthCondition;

    /** JSON array of {productId, productName, fit, reason}. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String recommendations;

    /** JSON array of strings: points the agent should confirm before quoting. */
    @Column(columnDefinition = "TEXT")
    private String agentChecks;

    @Column(columnDefinition = "TEXT")
    private String summary;

    /** AI or RULES: which engine produced the recommendation. */
    @Column(nullable = false, length = 10)
    private String source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private ConsultationApplication application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private ContactInquiry inquiry;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getAgeBand() { return ageBand; }
    public void setAgeBand(String ageBand) { this.ageBand = ageBand; }

    public String getMaritalStatus() { return maritalStatus; }
    public void setMaritalStatus(String maritalStatus) { this.maritalStatus = maritalStatus; }

    public String getChildrenBand() { return childrenBand; }
    public void setChildrenBand(String childrenBand) { this.childrenBand = childrenBand; }

    public String getGoals() { return goals; }
    public void setGoals(String goals) { this.goals = goals; }

    public String getExistingCoverage() { return existingCoverage; }
    public void setExistingCoverage(String existingCoverage) { this.existingCoverage = existingCoverage; }

    public String getBudgetBand() { return budgetBand; }
    public void setBudgetBand(String budgetBand) { this.budgetBand = budgetBand; }

    public String getOccupationClass() { return occupationClass; }
    public void setOccupationClass(String occupationClass) { this.occupationClass = occupationClass; }

    public String getHealthCondition() { return healthCondition; }
    public void setHealthCondition(String healthCondition) { this.healthCondition = healthCondition; }

    public String getRecommendations() { return recommendations; }
    public void setRecommendations(String recommendations) { this.recommendations = recommendations; }

    public String getAgentChecks() { return agentChecks; }
    public void setAgentChecks(String agentChecks) { this.agentChecks = agentChecks; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public ConsultationApplication getApplication() { return application; }
    public void setApplication(ConsultationApplication application) { this.application = application; }

    public ContactInquiry getInquiry() { return inquiry; }
    public void setInquiry(ContactInquiry inquiry) { this.inquiry = inquiry; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}

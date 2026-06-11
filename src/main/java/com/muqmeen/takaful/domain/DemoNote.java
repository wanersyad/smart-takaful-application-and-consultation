package com.muqmeen.takaful.domain;

/*
 * ============================================================================
 * PRESENTATION DEMO ENTITY — normally disabled.
 * ============================================================================
 *
 * Purpose: demonstrate live, during the PITA presentation, that the database
 * SCHEMA is driven by the Java code. Because the app runs with Hibernate
 * `ddl-auto: update`, simply enabling this @Entity class and restarting the
 * app makes Hibernate create a brand-new `demo_notes` table in Supabase
 * automatically — no CREATE TABLE SQL is ever written by hand.
 *
 * HOW TO RUN THE DEMO
 *   1. Before the presentation: uncomment the class body below (remove the
 *      surrounding block-comment markers around the imports and the class),
 *      commit, and let it deploy. Show that the `demo_notes` table appeared in
 *      Supabase → Table Editor. Then RE-COMMENT it and redeploy so the table is
 *      gone again — proving the code controls the schema in both directions.
 *   2. OR, do it live: uncomment, push, wait for the redeploy, refresh
 *      Supabase, and watch the new table appear. (A redeploy takes a few
 *      minutes — rehearse the timing first.)
 *
 * WHAT TO SAY
 *   "I added this one Java class. I never wrote any SQL. When the app started,
 *    Hibernate read the class and created this `demo_notes` table in PostgreSQL
 *    with these exact columns. The Java code defines the database structure."
 *
 * Leave this class commented out in normal operation so it does not create an
 * unused table in production.
 * ============================================================================
 */

/*
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "demo_notes")
public class DemoNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String body;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
*/

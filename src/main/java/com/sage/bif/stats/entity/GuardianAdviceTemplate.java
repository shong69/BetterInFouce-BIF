package com.sage.bif.stats.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "guardian_advice_template")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuardianAdviceTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "advice_template_id")
    private Long id;

    @Column(name = "okay_range", nullable = false)
    private String okayRange;

    @Column(name = "good_range", nullable = false)
    private String goodRange;

    @Column(name = "angry_range", nullable = false)
    private String angryRange;

    @Column(name = "down_range", nullable = false)
    private String downRange;

    @Column(name = "great_range", nullable = false)
    private String greatRange;

    @Column(name = "advice_text", columnDefinition = "TEXT", nullable = false)
    private String adviceText;

} 

package com.company.bikerent.maintenance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.company.bikerent.maintenance.domain.Technician;

@Repository
public interface TechnicianRepository extends JpaRepository<Technician, Long> {}

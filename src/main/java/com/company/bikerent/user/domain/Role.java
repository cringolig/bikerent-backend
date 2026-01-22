package com.company.bikerent.user.domain;

/**
 * User roles in the system.
 *
 * <ul>
 *   <li>USER - Regular customer who can rent bicycles
 *   <li>TECH - Technician who can perform repairs
 *   <li>ADMIN - Administrator with full access
 * </ul>
 */
public enum Role {
  USER,
  TECH,
  ADMIN
}

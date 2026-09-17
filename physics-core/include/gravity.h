#pragma once
#include "state.h"

namespace simulateuz {

// Computes gravitational acceleration vector based on Newton's Law of Universal Gravitation:
// F_g = G * (M * m) / r^2 => a_g = - (G * M / r^3) * r
Vector3D compute_gravitational_acceleration(const Vector3D& position);

// Computes atmospheric density via barometric formula:
// rho(h) = rho_0 * exp(-h / H_scale)
double compute_atmospheric_density(double altitude);

// Computes total acceleration including gravity, engine thrust, and atmospheric drag
Vector3D compute_total_acceleration(const RocketState& state);

// Computes the propellant depletion rate (kg/s) based on:
// dm/dt = - F_thrust / (g0 * Isp)
double compute_mass_flow_rate(const RocketState& state);

} // namespace simulateuz

#pragma once
#include <cmath>
#include <iostream>

namespace simulateuz {

// 3D Vector for position, velocity, and acceleration
struct Vector3D {
    double x = 0.0;
    double y = 0.0;
    double z = 0.0;

    Vector3D() = default;
    Vector3D(double x_, double y_, double z_) : x(x_), y(y_), z(z_) {}

    Vector3D operator+(const Vector3D& o) const { return {x + o.x, y + o.y, z + o.z}; }
    Vector3D operator-(const Vector3D& o) const { return {x - o.x, y - o.y, z - o.z}; }
    Vector3D operator*(double scalar) const { return {x * scalar, y * scalar, z * scalar}; }
    Vector3D operator/(double scalar) const { return {x / scalar, y / scalar, z / scalar}; }

    Vector3D& operator+=(const Vector3D& o) {
        x += o.x; y += o.y; z += o.z;
        return *this;
    }

    double magnitude() const {
        return std::sqrt(x * x + y * y + z * z);
    }

    Vector3D normalized() const {
        double m = magnitude();
        if (m < 1e-12) return {0, 0, 0};
        return *this / m;
    }
};

// Physical state representing the rocket in orbital flight
struct RocketState {
    Vector3D position;        // Position relative to Earth's center (meters)
    Vector3D velocity;        // Velocity vector (m/s)

    // Propulsion parameters
    double dry_mass = 25000.0;     // Rocket structural dry mass (kg)
    double fuel_mass = 400000.0;   // Usable propellant mass (kg)
    double thrust_force = 7600000.0;// Total engine thrust (Newtons, e.g. Falcon 9 ~7.6 MN)
    double isp = 311.0;            // Specific impulse at sea level / vacuum average (seconds)
    bool is_engine_on = true;      // Engine ignition state
    Vector3D thrust_direction = {0.0, 1.0, 0.0}; // Normalized thrust orientation

    double time = 0.0;             // Elapsed mission time (seconds)

    double total_mass() const {
        return dry_mass + fuel_mass;
    }
};

// Derivative container for RK4 integration
struct StateDerivative {
    Vector3D velocity;             // dr/dt = v
    Vector3D acceleration;         // dv/dt = a
    double mass_flow_rate = 0.0;   // dm/dt
};

// Astrophysical constants
constexpr double G_CONSTANT = 6.67430e-11;          // Gravitational constant G (m^3 kg^-1 s^-2)
constexpr double EARTH_MASS = 5.9722e24;             // Mass of Earth (kg)
constexpr double EARTH_RADIUS = 6371000.0;          // Mean Earth radius (m)
constexpr double STANDARD_GRAVITY_G0 = 9.80665;     // Standard acceleration due to gravity (m/s^2)
constexpr double EARTH_MU = G_CONSTANT * EARTH_MASS;// Standard gravitational parameter (m^3 / s^2)

} // namespace simulateuz

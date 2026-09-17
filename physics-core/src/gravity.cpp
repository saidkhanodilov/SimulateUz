#include "gravity.h"
#include <cmath>
#include <algorithm>

namespace simulateuz {

Vector3D compute_gravitational_acceleration(const Vector3D& position) {
    double r = position.magnitude();
    if (r < 1.0) return {0.0, 0.0, 0.0};

    // a = - (mu / r^3) * r
    double factor = -EARTH_MU / (r * r * r);
    return position * factor;
}

double compute_atmospheric_density(double altitude) {
    if (altitude < 0.0) altitude = 0.0;
    if (altitude > 140000.0) return 0.0; // Above 140 km, atmospheric drag is negligible

    constexpr double rho0 = 1.225;        // Sea-level atmospheric density (kg/m^3)
    constexpr double scale_height = 8500.0; // Earth scale height (meters)
    return rho0 * std::exp(-altitude / scale_height);
}

Vector3D compute_total_acceleration(const RocketState& state) {
    // 1. Gravitational acceleration from Earth
    Vector3D a_gravity = compute_gravitational_acceleration(state.position);

    // 2. Thrust acceleration
    Vector3D a_thrust = {0.0, 0.0, 0.0};
    double current_mass = state.total_mass();
    if (state.is_engine_on && state.fuel_mass > 0.0 && current_mass > 0.0) {
        Vector3D thrust_dir = state.thrust_direction.normalized();
        a_thrust = thrust_dir * (state.thrust_force / current_mass);
    }

    // 3. Atmospheric aerodynamic drag: F_drag = 0.5 * rho * v^2 * Cd * A
    double altitude = state.position.magnitude() - EARTH_RADIUS;
    double rho = compute_atmospheric_density(altitude);
    Vector3D a_drag = {0.0, 0.0, 0.0};

    double v_mag = state.velocity.magnitude();
    if (rho > 1e-9 && v_mag > 1e-3 && current_mass > 0.0) {
        constexpr double drag_coeff_cd = 0.3; // Streamlined rocket drag coefficient
        constexpr double cross_section_area = 10.5; // ~3.7m diameter rocket cross section (m^2)
        double drag_force_mag = 0.5 * rho * v_mag * v_mag * drag_coeff_cd * cross_section_area;
        Vector3D velocity_dir = state.velocity.normalized();
        a_drag = velocity_dir * (-drag_force_mag / current_mass);
    }

    return a_gravity + a_thrust + a_drag;
}

double compute_mass_flow_rate(const RocketState& state) {
    if (!state.is_engine_on || state.fuel_mass <= 0.0 || state.isp <= 0.0) {
        return 0.0;
    }
    // dm/dt = - F_thrust / (g0 * Isp)
    return -(state.thrust_force / (STANDARD_GRAVITY_G0 * state.isp));
}

} // namespace simulateuz

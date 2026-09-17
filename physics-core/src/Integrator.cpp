#include "Integrator.h"
#include "gravity.h"
#include <algorithm>

namespace simulateuz {

StateDerivative RK4Integrator::evaluate(const RocketState& state) {
    StateDerivative deriv;
    deriv.velocity = state.velocity;
    deriv.acceleration = compute_total_acceleration(state);
    deriv.mass_flow_rate = compute_mass_flow_rate(state);
    return deriv;
}

RocketState RK4Integrator::step(const RocketState& current, double dt) {
    if (dt <= 0.0) return current;

    // k1 = f(t, y)
    StateDerivative k1 = evaluate(current);

    // k2 = f(t + dt/2, y + dt/2 * k1)
    RocketState state_k2 = current;
    state_k2.position = current.position + k1.velocity * (0.5 * dt);
    state_k2.velocity = current.velocity + k1.acceleration * (0.5 * dt);
    state_k2.fuel_mass = std::max(0.0, current.fuel_mass + k1.mass_flow_rate * (0.5 * dt));
    state_k2.time = current.time + 0.5 * dt;
    StateDerivative k2 = evaluate(state_k2);

    // k3 = f(t + dt/2, y + dt/2 * k2)
    RocketState state_k3 = current;
    state_k3.position = current.position + k2.velocity * (0.5 * dt);
    state_k3.velocity = current.velocity + k2.acceleration * (0.5 * dt);
    state_k3.fuel_mass = std::max(0.0, current.fuel_mass + k2.mass_flow_rate * (0.5 * dt));
    state_k3.time = current.time + 0.5 * dt;
    StateDerivative k3 = evaluate(state_k3);

    // k4 = f(t + dt, y + dt * k3)
    RocketState state_k4 = current;
    state_k4.position = current.position + k3.velocity * dt;
    state_k4.velocity = current.velocity + k3.acceleration * dt;
    state_k4.fuel_mass = std::max(0.0, current.fuel_mass + k3.mass_flow_rate * dt);
    state_k4.time = current.time + dt;
    StateDerivative k4 = evaluate(state_k4);

    // Combine RK4 weighted average: y_{n+1} = y_n + dt/6 * (k1 + 2k2 + 2k3 + k4)
    RocketState next = current;
    Vector3D d_pos = (k1.velocity + k2.velocity * 2.0 + k3.velocity * 2.0 + k4.velocity) * (dt / 6.0);
    Vector3D d_vel = (k1.acceleration + k2.acceleration * 2.0 + k3.acceleration * 2.0 + k4.acceleration) * (dt / 6.0);
    double d_fuel = (k1.mass_flow_rate + k2.mass_flow_rate * 2.0 + k3.mass_flow_rate * 2.0 + k4.mass_flow_rate) * (dt / 6.0);

    next.position += d_pos;
    next.velocity += d_vel;
    next.fuel_mass = std::max(0.0, current.fuel_mass + d_fuel);
    next.time = current.time + dt;

    if (next.fuel_mass <= 0.0) {
        next.is_engine_on = false;
        next.thrust_force = 0.0;
    }

    // Ground collision clamping (launch pad / Earth surface)
    double r = next.position.magnitude();
    if (r < EARTH_RADIUS) {
        next.position = next.position.normalized() * EARTH_RADIUS;
        // If moving inward, zero out radial negative velocity
        double radial_vel = (next.velocity.x * next.position.x + 
                             next.velocity.y * next.position.y + 
                             next.velocity.z * next.position.z) / EARTH_RADIUS;
        if (radial_vel < 0) {
            next.velocity = next.velocity - next.position.normalized() * radial_vel;
        }
    }

    return next;
}

} // namespace simulateuz

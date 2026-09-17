#pragma once
#include "state.h"

namespace simulateuz {

class RK4Integrator {
public:
    // Evaluates state derivatives at a given snapshot
    static StateDerivative evaluate(const RocketState& state);

    // Advances the physical state by delta time dt using Runge-Kutta 4th Order
    static RocketState step(const RocketState& current, double dt);
};

} // namespace simulateuz

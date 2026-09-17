#include "state.h"
#include "gravity.h"
#include "Integrator.h"
#include "PhysicsBridge.h"
#include <iostream>
#include <iomanip>

int main(int argc, char* argv[]) {
    std::cout << "========================================================\n";
    std::cout << "  SimulateUz C++ Physics Core - Headless Launch Engine\n";
    std::cout << "  Runge-Kutta 4th Order (RK4) Orbital & Thrust Mechanics\n";
    std::cout << "========================================================\n\n";

    init_simulation();

    double dt = 0.1; // 100 ms physics timestep
    double total_sim_time = 100.0; // 100 seconds burn test
    int steps = static_cast<int>(total_sim_time / dt);

    std::cout << std::fixed << std::setprecision(2);
    std::cout << "Time(s) | Altitude(km) | Velocity(m/s) | Fuel(kg) | Mass(kg) | Thrust(kN)\n";
    std::cout << "------------------------------------------------------------------------\n";

    for (int i = 0; i <= steps; ++i) {
        double current_t = i * dt;
        if (i % 100 == 0 || i == steps) { // Print every 10 seconds
            double alt_km = get_altitude() / 1000.0;
            double vel = get_velocity();
            double fuel = get_fuel_mass();
            double dry = get_dry_mass();
            double thrust_kn = get_thrust() / 1000.0;

            std::cout << std::setw(7) << current_t << " | "
                      << std::setw(12) << alt_km << " | "
                      << std::setw(13) << vel << " | "
                      << std::setw(8) << fuel << " | "
                      << std::setw(8) << (dry + fuel) << " | "
                      << std::setw(10) << thrust_kn << "\n";
        }
        step_simulation(dt);
    }

    std::cout << "\n[SUCCESS] Headless 100s launch burn simulation completed successfully.\n";
    return 0;
}

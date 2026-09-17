#pragma once

#ifdef __cplusplus
extern "C" {
#endif

// C-compatible interface functions as requested
void init_simulation();
void step_simulation(double dt);
void set_thrust(double magnitude);
void set_engine_state(int state);
void set_thrust_direction(double x, double y, double z);
double get_altitude();
double get_velocity();
double get_fuel_mass();
double get_dry_mass();
double get_thrust();
double get_pos_x();
double get_pos_y();
double get_pos_z();
double get_vel_x();
double get_vel_y();
double get_vel_z();
double get_time();

#ifdef __cplusplus
}
#endif

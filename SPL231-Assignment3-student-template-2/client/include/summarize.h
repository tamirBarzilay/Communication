#pragma once
#include <string>
#include <iostream>
#include <map>
#include <vector>
using std::string;
// TODO: implement the STOMP protocol
class summarize
{
private:
string game_name;
string team_a_name;
string team_b_name;
std::map<std::string,std::string> general_stats_map;
std::map<std::string,std::string> team_a_stats_map;
std::map<std::string,std::string> team_b_stats_map;
          //time      
std::string game_event_reports;
public:
summarize(string game_name);

void add_event(string send_frame);

string get_full_summary();

void set_general_game_updates(string general_updates);

void set_team_a_game_updates(string team_a_updates);

void set_team_b_game_updates(string team_b_updates);

void add_event_to_reports(string s);

};
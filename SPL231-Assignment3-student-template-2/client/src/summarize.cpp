
#include "../include/summarize.h"
#include <sstream>
#include <vector>

using std::string;

summarize::summarize(string game_name) : game_name(game_name), team_a_name(), team_b_name(), general_stats_map(), team_a_stats_map(), team_b_stats_map(), game_event_reports("")
{
   int index = game_name.find_first_of('_');
   team_a_name = game_name.substr(0, index);
   team_b_name = game_name.substr(index + 1);
}

void summarize::add_event(string send_frame)
{ // frame starts from "event name:"
   std::stringstream s(send_frame);
   std::string segment;
   std::vector<std::string> vec;
   // create a vector that each element is a line
   while (std::getline(s, segment, '\n'))
   {
      vec.push_back(segment);
   }
   string general_updates = "";
   string team_a_updates = "";
   string team_b_updates = "";
   int team_a_line_num = -1;
   int team_b_line_num = -1;
   for (int i = 2; i < (int)vec.size(); i++)
   {
      if (vec[i].find("team") != string::npos)
      {
         team_a_line_num = i;
         break;
      }
      general_updates = general_updates + vec[i] + "\n";
   }
   for (int i = team_a_line_num; i < (int)vec.size(); i++)
   {
      if (vec[i].find("team b") != string::npos)
      {
         team_b_line_num = i;
         break;
      }
      team_a_updates = team_a_updates + vec[i] + "\n";
   }
   for (int i = team_b_line_num; i < (int)vec.size(); i++)
   {
      if (vec[i].find("description") != string::npos)
      {
         break;
      }
      team_b_updates = team_b_updates + vec[i] + "\n";
   }
   set_general_game_updates(general_updates);
   set_team_a_game_updates(team_a_updates);
   set_team_b_game_updates(team_b_updates);
   add_event_to_reports(send_frame);
}

string summarize::get_full_summary()
{

   string general_s = "";
   for (auto &&i : general_stats_map)
   {
      general_s = general_s + i.first + ":" + i.second + "\n";
   }

   string team_a_s = "";
   for (auto &&i : team_a_stats_map)
   {
      team_a_s = team_a_s + i.first + ":" + i.second + "\n";
   }

   string team_b_s = "";
   for (auto &&i : team_b_stats_map)
   {
      team_b_s = team_b_s + i.first + ":" + i.second + "\n";
   }

   string full_summary =
       team_a_name + " vs " + team_b_name + "\n" +
       "Game stats:\n" +
       "General stats:\n" +
       general_s +
       team_a_name + " stats:\n" +
       team_a_s +
       team_b_name + " stats:\n" +
       team_b_s +
       "Game event reports:\n" +
       game_event_reports;

   return full_summary;
}

void summarize::set_general_game_updates(string general_updates)
{
   std::stringstream s(general_updates);
   std::string segment;
   std::vector<std::string> updates_vec;
   // create a vector that each element is a line
   while (std::getline(s, segment, '\n'))
   {
      updates_vec.push_back(segment);
   }
   updates_vec.erase(updates_vec.begin());
   for (auto &&i : updates_vec)
   {
      string stat_name = i.substr(0, i.find_first_of(':'));
      string stat_val = i.substr(i.find_first_of(':') + 1);
      // if the key doesn't exist create it , otherwise it update it.
      general_stats_map[stat_name] = stat_val;
   }
}

void summarize::set_team_a_game_updates(string team_a_updates)
{

   std::stringstream s(team_a_updates);
   std::string segment;
   std::vector<std::string> updates_vec;
   // create a vector that each element is a line
   while (std::getline(s, segment, '\n'))
   {
      updates_vec.push_back(segment);
   }
   updates_vec.erase(updates_vec.begin());
   for (auto &&i : updates_vec)
   {
      string stat_name = i.substr(0, i.find_first_of(':'));
      string stat_val = i.substr(i.find_first_of(':') + 1);
      // if the key doesn't exist it create it , otherwise it update it.
      team_a_stats_map[stat_name] = stat_val;
   }
}

void summarize::set_team_b_game_updates(string team_b_updates)
{
   std::stringstream s(team_b_updates);
   std::string segment;
   std::vector<std::string> updates_vec;
   // create a vector that each element is a line
   while (std::getline(s, segment, '\n'))
   {
      updates_vec.push_back(segment);
   }
   updates_vec.erase(updates_vec.begin());
   for (auto &&i : updates_vec)
   {
      string stat_name = i.substr(0, i.find_first_of(':'));
      string stat_val = i.substr(i.find_first_of(':') + 1);
      // if the key doesn't exist it create it , otherwise it update it.
      team_b_stats_map[stat_name] = stat_val;
   }
}

void summarize::add_event_to_reports(string send_frame)
{
   std::stringstream s(send_frame);
   std::string segment;
   std::vector<std::string> vec;
   // create a vector that each element is a line
   while (std::getline(s, segment, '\n'))
   {
      vec.push_back(segment);
   }
   string event_name = vec[0].substr(11);
   string time = vec[1].substr(5);
   string event_description = "";
   bool des = false;
   for (int i = 2; i < (int)vec.size(); i++)
   {
      if (des)
         event_description = event_description + vec[i] + "\n";
      if (vec[i].find("description") != string::npos)
         des = true;
   }
   game_event_reports = game_event_reports + "\n\n" + time + " - " + event_name + "\n\n" + event_description;
}

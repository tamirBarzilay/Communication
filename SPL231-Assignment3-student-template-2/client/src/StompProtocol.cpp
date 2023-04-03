
#include "../include/StompProtocol.h"
using std::string;
using namespace std;

StompProtocol::StompProtocol(ConnectionHandler &connectionHandler, std::mutex &_mutex) : receipt_waiting(), topic_subscription(), summarize_map(), subId(0), receiptId(0), user_name(""), loggedIn(false), connectionHandler(connectionHandler), disconnect_receipt_id(-1), _mutex(_mutex) {}

bool StompProtocol::process(string &command)
{
    if (command == "logout")
    {
        string s = "DISCONNECT\nreceipt:" + std::to_string(receiptId) + "\n\n";
        if (!connectionHandler.sendLine(s)) // error
        {
            return true;
        }
        disconnect_receipt_id = receiptId;
        receiptId++;
        return false;
    }

    int first_space = command.find_first_of(' ');
    string command_type = command.substr(0, first_space);
    if (command_type == "login")
    {
        std::stringstream strem(command);
        std::string segment;
        std::vector<std::string> command_vec;
        // create a vector that each element is a line
        while (std::getline(strem, segment, ' '))
        {
            command_vec.push_back(segment);
        }
        string temp_user_name = command_vec[2];
        user_name = temp_user_name;
        string passcode = command_vec[3];
        string s = "CONNECT\naccept-version:1.2\nhost:stomp.cs.bgu.ac.il\nlogin:" + temp_user_name + "\npasscode:" + passcode + "\n\n";
        if (!connectionHandler.sendLine(s)) // error
        {
            return true;
        }
    }

    else if (command_type == "join")
    {
        string topic = command.substr(first_space + 1);
        string subscriptiobId = std::to_string(subId);
        string recId = std::to_string(receiptId);
        string s = "SUBSCRIBE\ndestination:/" + topic + "\nid:" + subscriptiobId + "\nreceipt:" + recId + "\n\n";
        if (!connectionHandler.sendLine(s)) // error
        {
            return true;
        }
        // saving info for the receipt message from server-syncronize needed
        std::lock_guard<std::mutex> lock(_mutex);
        std::map<string, int> map;
        map[topic] = subId;
        receipt_waiting[receiptId] = map;
        subId++;
        receiptId++;
    }
    else if (command_type == "exit")
    {
        string topic = command.substr(first_space + 1);
        if (topic_subscription.find(topic) == topic_subscription.end())
        {
            // not subscribed
            std::cout << "Not necessary to unsubscribe " + topic << std::endl;
        }
        else
        {
            int intSubscriptiobId = topic_subscription.find(topic)->second;
            string subscriptiobId = std::to_string(intSubscriptiobId);
            string recId = std::to_string(receiptId);
            string s = "UNSUBSCRIBE\nid:" + subscriptiobId + "\nreceipt:" + recId + "\n\n";
            if (!connectionHandler.sendLine(s)) // error
            {
                return true;
            }
            // saving info for the receipt message from server
            std::lock_guard<std::mutex> lock(_mutex);
            std::map<string, int> map;
            map[topic] = intSubscriptiobId;
            receipt_waiting[receiptId] = map;
            receiptId++;
        }
    }
    else if (command_type == "report")
    {
        names_and_events file = parseEventsFile(command.substr(first_space + 1));
        // send a send frame for each event
        bool error = send_reports(file);
        if (error) // error
        {
            return true;
        }
    }
    else if (command_type == "summary")
    { // command type = summarize
        std::stringstream s(command);
        std::string segment;
        std::vector<std::string> command_vec;
        while (std::getline(s, segment, ' '))
        {
            command_vec.push_back(segment);
        }
        string game_name = command_vec.at(1);
        string user_n = command_vec.at(2);
        ofstream file;
        string file_name=command_vec.at(3);
        file.open(file_name);

        // file???????????????????????????????????????????????????????????
        if (summarize_map.find(user_n) == summarize_map.end())
            std::cout << "user name:" + user_n + " has no summarize info" << std::endl;
        else if (summarize_map.find(user_n)->second.find(game_name) == summarize_map.find(user_n)->second.end())
            std::cout << "user name:" + user_n + " has no summarize info on the topic:" + game_name << std::endl;
        else
        {
            string summary = summarize_map.find(user_n)->second.find(game_name)->second.get_full_summary();
            if (file)
            {
                file << summary;
                file.close();
            }
            else{
            ofstream new_file(file_name);
            new_file<<summary;
            new_file.close();
            }
        }
    }
    else
        std::cout << "unfamiliar command" << std::endl;
    return false;
}

bool StompProtocol::process_answer(string &answer)
{
    if (answer.find('\n') == std::string::npos)
        std::cout << answer << std::endl;
    else
    {
        int end_of_first_line = answer.find_first_of('\n');
        string command_type = answer.substr(0, end_of_first_line);
        if (command_type == "CONNECTED")
        {
            loggedIn = true;
            std::cout << "Login successful" << std::endl;
            return true;
        }
        if (command_type == "ERROR")
        {
            std::stringstream s(answer);
            std::string segment;
            std::vector<std::string> ans_vec;
            // create a vector that each element is a line
            while (std::getline(s, segment, '\n'))
            {
                ans_vec.push_back(segment);
            }
            std::cout << ans_vec[1] << std::endl;
            std::cout << "press ENTER to continue and then try to login again" << std::endl;
            return false;
        }
        if (command_type == "RECEIPT")
        {
            int end_of_second_line = answer.substr(end_of_first_line + 1).find_first_of('\n');
            string st_id = answer.substr(answer.find("id") + 3, end_of_second_line);

            int id = stoi(st_id);
            if (id == disconnect_receipt_id) // DISCONNECTED receipt
            {
                return false;
            }
            else
            {                                             // subscribe receipt or unsubscribe receipt was received.
                std::lock_guard<std::mutex> lock(_mutex); // sync
                std::map<std::string, int> map = receipt_waiting.find(id)->second;
                string topic = map.begin()->first;
                // int subscription_id = map.begin()->second;
                receipt_waiting.erase(id);

                //// user is already subscribed to the topic-unsubscribe receipt was received
                if (topic_subscription.find(topic) != topic_subscription.end())
                {
                    // delete all data related to this topic: is it neccesary???
                    topic_subscription.erase(topic);
                    for (auto &&i : summarize_map) // delete summarize data
                    {
                        i.second.erase(topic);
                    }
                    std::cout << "Exited channel" + topic << std::endl;
                }
                else
                { // subscribe
                    topic_subscription[topic] = id;
                    std::cout << "Joined channel " + topic << std::endl;
                }
            }
            return true;
        }
        if (command_type == "MESSAGE")
        { // command_type=="MESSAGE"
            std::stringstream s(answer);
            std::string segment;
            std::vector<std::string> answer_vec;
            while (std::getline(s, segment, '\n'))
            {
                answer_vec.push_back(segment);
            }
            string message_topic = answer_vec[3].substr(13);

            string message_user_name = answer_vec[5].substr(5);
            string event = "";
            for (int i = 8; i < (int)answer_vec.size(); i++)
            {
                event = event + answer_vec[i] + "\n";
            }

            // no summ info from that user
            if (summarize_map.find(message_user_name) == summarize_map.end())
            {
                std::map<std::string, summarize> map;
                summarize_map[message_user_name] = map;
            }
            // no summ info on that topic in this user's section
            if (summarize_map.find(message_user_name)->second.find(message_topic) == summarize_map.find(message_user_name)->second.end())
            {
                // std::map<string,summarize> map = summarize_map.find(message_user_name)->second;
                // map[message_topic] =  summarize sum(message_topic)
                (summarize_map.find(message_user_name)->second).insert(std::pair<string, summarize>(message_topic, summarize(message_topic)));
                summarize_map.find(message_user_name)->second.find(message_topic)->second.add_event(event);
            }
            else
                summarize_map.find(message_user_name)->second.find(message_topic)->second.add_event(event);
        }
    }
    return true;
}
bool StompProtocol::send_reports(names_and_events &file)
{
    string team_a = file.team_a_name;
    string team_b = file.team_b_name;
    for (auto &&i : file.events) // for each event
    {
        string general_game_upd = "";
        for (auto &&j : i.get_game_updates())
        {
            general_game_upd = general_game_upd + "  " + j.first + ":" + j.second + "\n";
        }
        string team_a_upd = "";
        for (auto &&j : i.get_team_a_updates())
        {
            team_a_upd = team_a_upd + "  " + j.first + ":" + j.second + "\n";
        }
        string team_b_upd = "";
        for (auto &&j : i.get_team_b_updates())
        {
            team_b_upd = team_b_upd + "  " + j.first + ":" + j.second + "\n";
        }

        string send = "SEND\ndestination:/" + team_a + "_" + team_b + "\n\nuser:" + user_name +
                      "\nteam a:" + team_a + "\nteam b:" + team_b + "\nevent name:" + i.get_name() + "\ntime:" + std::to_string(i.get_time()) + "\ngeneral game updates:\n" + general_game_upd + "team a updates:\n" + team_a_upd + "team b updates:\n" + team_b_upd + "description:\n" + i.get_discription() + "\n";

        if (!connectionHandler.sendLine(send)) // error
        {
            return true;
        }
    }
    return false;
}
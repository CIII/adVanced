set :application, "advanced"

set :scm, :git

role :web, "10.60.100.43"                   # Your HTTP server, Apache/etc
role :app, "10.60.100.43"                   # This may be the same as your `Web` server
#role :db,  "10.60.120.201", :primary => true # This is where Rails migrations will run

set :port, 22

# The server's user for deploys
set :user, "application"
set :use_sudo, false

set :ssh_options, { :forward_agent => true }

set :branch, "master"

set :deploy_via, :remote_cache

set :keep_releases, 5

# the directory where capistrano will setup it's env and clone your VCS and deploy current version etc...
# read this for more info about Capistrano deploy https://github.com/mpasternacki/capistrano-documentation-support-files/raw/master/default-execution-path/Capistrano%20Execution%20Path.jpg
set :deploy_to, "/home/#{user}/apps/play/#{application}"

set :shared_path, "#{deploy_to}/shared"
set :app_pid, "#{shared_path}/pids/server.pid"
set :app_path, "#{deploy_to}/current"

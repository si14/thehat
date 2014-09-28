deploy_production:
	curl -XPOST -d "source=`git config user.name`&from_address=`git config user.email`&subject=started deploying to production&content=`git log --pretty=format:'%s %H' -n 1`" https://api.flowdock.com/v1/messages/team_inbox/09fcb557e55453ffa7958db10f1b2fee
	lein do clean, cljsbuild clean, cljsbuild once release, minify-assets
	ansible-playbook -i hosts -u cloudsigma infrastructure/deploy.yml
	curl -XPOST -d "source=`git config user.name`&from_address=`git config user.email`&subject=deployed to production&content=`git log --pretty=format:'%s %H' -n 1`" https://api.flowdock.com/v1/messages/team_inbox/09fcb557e55453ffa7958db10f1b2fee

setup_production:
	ansible-playbook -i hosts -u cloudsigma -K infrastructure/setup.yml

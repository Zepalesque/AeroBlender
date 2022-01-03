@Library('forge-shared-library')_

pipeline {
    options {
        disableConcurrentBuilds()
    }
    agent {
        docker {
            image 'gradle:7-jdk16'
        }
    }
    environment {
        GRADLE_ARGS = '--no-daemon --console=plain' // No daemon for now as FG3 kinda derps. //'-Dorg.gradle.daemon.idletimeout=5000'
        JENKINS_HEAD = 'https://wiki.jenkins-ci.org/download/attachments/2916393/headshot.png'
    }

    stages {
        stage('fetch') {
            steps {
                checkout scm
            }
        }
        stage('setup') {
            steps {
                withGradle {
                    sh './gradlew ${GRADLE_ARGS} --refresh-dependencies'
                }
                script {
                    env.MYVERSION = sh(returnStdout: true, script: './gradlew :properties -q | grep "^version:" | awk \'{print $2}\'').trim()
                }
            }
        }
        stage('changelog') {
            when {
                not {
                    changeRequest()
                }
            }
            steps {
                writeChangelog(currentBuild, "build/TerraBlender-${env.MYVERSION}-changelog.txt")
            }
        }
        stage('publish') {
            when {
                not {
                    changeRequest()
                }
            }
            environment {
                CURSE_API_KEY = credentials('curse-api-key')
                MAVEN_USER = credentials('forge-maven-user')
                MAVEN_PASSWORD = credentials('forge-maven-password')
            }
            steps {
                withGradle {
                    sh './gradlew ${GRADLE_ARGS} curseforge publish -PcurseApiKey=${CURSE_API_KEY} -PmavenUser=${MAVEN_USER} -PmavenPassword${MAVEN_PASSWORD}'
                }
            }
        }
    }
}
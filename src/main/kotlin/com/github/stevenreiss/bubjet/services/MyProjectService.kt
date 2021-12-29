package com.github.stevenreiss.bubjet.services

import com.intellij.openapi.project.Project
import com.github.stevenreiss.bubjet.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}

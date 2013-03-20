# -----------------------------------------------
# MAIN
# -----------------------------------------------
# DISCLAMER :
# If you're used to Backbone.js, you may be
# confused by the absence of models, but the goal
# of this sample is to demonstrate some features
# of Play including the template engine.
# I'm not using client-side templating nor models
# for this purpose, and I do not recommend this
# behavior for real life projects.
# -----------------------------------------------

# Just a log helper
log = (args...) ->
    console.log.apply console, args if console.log?

# ------------------------------- DROP DOWN MENUS
$(".options dt, .users dt").live "click", (e) ->
    e.preventDefault()
    if $(e.target).parent().hasClass("opened")
        $(e.target).parent().removeClass("opened")
    else
        $(e.target).parent().addClass("opened")
        $(document).one "click", ->
            $(e.target).parent().removeClass("opened")
    false

# --------------------------------- EDIT IN PLACE
$.fn.editInPlace = (method, options...) ->
    this.each ->
        methods = 
            # public methods
            init: (options) ->
                valid = (e) =>
                    newValue = @input.val()
                    options.onChange.call(options.context, newValue)
                cancel = (e) =>
                    @el.show()
                    @input.hide()
                @el = $(this).dblclick(methods.edit)
                @input = $("<input type='text' />")
                    .insertBefore(@el)
                    .keyup (e) ->
                        switch(e.keyCode)
                            # Enter key
                            when 13 then $(this).blur()
                            # Escape key
                            when 27 then cancel(e)
                    .blur(valid)
                    .hide()
            edit: ->
                @input
                    .val(@el.text())
                    .show()
                    .focus()
                    .select()
                @el.hide()
            close: (newName) ->
                @el.text(newName).show()
                @input.hide()
        # jQuery approach: http://docs.jquery.com/Plugins/Authoring
        if ( methods[method] )
            return methods[ method ].apply(this, options)
        else if (typeof method == 'object')
            return methods.init.call(this, method)
        else
            $.error("Method " + method + " does not exist.")

# ---------------------------------------- DRAWER
class Drawer extends Backbone.View
    initialize: ->
        $("#newGroup").click @addGroup
        # HTML is our model
        @el.children("li").each (i,group) ->
            new Group
                el: $(group)
            $("li",group).each (i,project) ->
                new Project
                    el: $(project)
    addGroup: ->
        r = jsRoutes.controllers.Projects.addGroup()
        $.ajax
            url: r.url
            type: r.type
            success: (data) ->
                _view = new Group
                    el: $(data).appendTo("#projects")
                _view.el.find(".groupName").editInPlace("edit")
            error: (err) ->
                # TODO: Deal with

# ---------------------------------------- GROUPS
class Group extends Backbone.View
    events:
        "click    .toggle"          : "toggle"
        "click    .newProject"      : "newProject"
        "click    .deleteGroup"     : "deleteGroup"
    initialize: ->
        @id = @el.attr("data-group")
        @name = $(".groupName", @el).editInPlace
            context: this
            onChange: @renameGroup
    newProject: (e) ->
        e.preventDefault()
        @el.removeClass("closed")
        r = jsRoutes.controllers.Projects.add()
        $.ajax
            url: r.url
            type: r.type
            context: this
            data:
                group: @el.attr("data-group")
            success: (tpl) ->
                _list = $("ul",@el)
                _view = new Project
                    el: $(tpl).appendTo(_list)
                _view.el.find(".name").editInPlace("edit")
            error: (err) ->
                $.error("Error: " + err)
    deleteGroup: (e) ->
        e.preventDefault()
        false if (!confirm "Remove group and projects inside?")
        id = @el.attr("data-group-id")
        @loading(true)
        r = jsRoutes.controllers.Projects.deleteGroup(@id)
        $.ajax
            url: r.url
            type: r.type
            context: this
            success: ->
                @el.remove()
                @loading(false)
            error: (err) ->
                @loading(false)
                $.error("Error: " + err)
    renameGroup: (name) =>
        @loading(true)
        r = jsRoutes.controllers.Projects.renameGroup(@id)
        $.ajax
            url: r.url
            type: r.type
            context: this
            data:
                name: name
            success: (data) ->
                @loading(false)
                @name.editInPlace("close", data)
                @el.attr("data-group", data)
                @id = @el.attr("data-group")
            error: (err) ->
                @loading(false)
                $.error("Error: " + err)
    toggle: (e) ->
        e.preventDefault()
        @el.toggleClass("closed")
        false
    loading: (display) ->
        if (display)
            @el.children(".options").hide()
            @el.children(".loader").show()
        else
            @el.children(".options").show()
            @el.children(".loader").hide()

# --------------------------------------- PROJECT
class Project extends Backbone.View
    events:
        "click      .delete"    : "deleteProject"
    initialize: ->
        @id = @el.attr("data-project")
        @name = $(".name", @el).editInPlace
            context: this
            onChange: @renameProject
    renameProject: (name) ->
        @loading(true)
        r = jsRoutes.controllers.Projects.rename(@id)
        $.ajax
            url: r.url
            type: r.type
            context: this
            data:
                name: name
            success: (data) ->
                @loading(false)
                @name.editInPlace("close", data)
            error: (err) ->
                @loading(false)
                $.error("Error: " + err)
    deleteProject: (e) ->
        e.preventDefault()
        @loading(true)
        r = jsRoutes.controllers.Projects.delete(@id)
        $.ajax
            url: r.url
            type: r.type
            context: this
            success: ->
                @el.remove()
                @loading(false)
            error: (err) ->
                @loading(false)
                $.error("Error: " + err)
        false
    loading: (display) ->
        if (display)
            @el.children(".delete").hide()
            @el.children(".loader").show()
        else
            @el.children(".delete").show()
            @el.children(".loader").hide()

# ---------------------------------------- ROUTER
class AppRouter extends Backbone.Router
    initialize: ->
        @currentApp = new Tasks
            el: $("#main")
    routes:
        "/"                          : "index"
        "/projects/:project/tasks"   : "tasks"
    index: ->
        # show dashboard
        $("#main").load "/ #main"
    tasks: (project) ->
        # load project || display app
        currentApp = @currentApp
        $("#main").load "/projects/" + project + "/tasks", (tpl) ->
            currentApp.render(project)

# ----------------------------------------- TASKS
class Tasks extends Backbone.View
    events:
        "click .newFolder"              : "newFolder"
        "click .list .action"           : "removeUser"
        "click .addUserList .action"    : "addUser"
    render: (project) ->
        @project = project
        # HTML is our model
        @folders = $.map $(".folder", @el), (folder) =>
            new TaskFolder
                el: $(folder)
                project: @project
    newFolder: (e) ->
        e.preventDefault()
        r = jsRoutes.controllers.Tasks.addFolder(@project)
        $.ajax
            url: r.url
            type: r.type
            context: this
            success: (tpl) ->
                newFolder = new TaskFolder
                    el: $(tpl).insertBefore(".newFolder")
                    project: @project
                newFolder.el.find("header > h3").editInPlace("edit")
             error: (err) ->
                $.error("Error: " + err)
        false
    removeUser: (e) ->
        e.preventDefault()
        r = jsRoutes.controllers.Projects.removeUser(@project)
        $.ajax
            url: r.url
            type: r.type
            context: this
            data:
                user: $(e.target).parent().data('user-id')
            success: ->
                $(e.target).parent().appendTo(".addUserList")
             error: (err) ->
                $.error("Error: " + err)
        false
    addUser: (e) ->
        e.preventDefault()
        r = jsRoutes.controllers.Projects.addUser(@project)
        $.ajax
            url: r.url
            type: r.type
            context: this
            data:
                user: $(e.target).parent().data('user-id')
            success: ->
                $(e.target).parent().appendTo(".users .list")
            error: (err) ->
                $.error("Error: " + err)
        false

# ---------------------------------- TASKS FOLDER
class TaskFolder extends Backbone.View
    events:
        "click .deleteCompleteTasks"    : "deleteCompleteTasks"
        "click .deleteAllTasks"         : "deleteAllTasks"
        "click .deleteFolder"           : "deleteFolder"
        "change header>input"           : "toggleAll"
        "submit .addTask"               : "newTask"
    initialize: (options) =>
        @project = options.project
        @tasks = $.map $(".list li",@el), (item)=>
            newTask = new TaskItem
                el: $(item)
                folder: @
            newTask.bind("change", @refreshCount)
            newTask.bind("delete", @deleteTask)
        @counter = @el.find(".counter")
        @id = @el.attr("data-folder-id")
        @name = $("header > h3", @el).editInPlace
            context: this
            onChange: @renameFolder
        @refreshCount()
    newTask: (e) =>
        e.preventDefault()
        $(document).focus() # temporary disable form
        form = $(e.target)
        taskBody = $("input[name=taskBody]", form).val()
        r = jsRoutes.controllers.Tasks.add(@project, @id)
        $.ajax
            url: r.url
            type: r.type
            context: this
            data:
                title: $("input[name=taskBody]", form).val()
                dueDate: $("input[name=dueDate]", form).val()
                assignedTo: 
                    email: $("input[name=assignedTo]", form).val()
            success: (tpl) ->
                newTask = new TaskItem(el: $(tpl), folder: @)
                @el.find("ul").append(newTask.el)
                @tasks.push(newTask)
                form.find("input[type=text]").val("").first().focus()
            error: (err) ->
                alert "Something went wrong:" + err
        false
    renameFolder: (name) =>
        @loading(true)
        r = jsRoutes.controllers.Tasks.renameFolder(@project, @id)
        $.ajax
            url: r.url
            type: r.type
            context: this
            data:
                name: name
            success: (data) ->
                @loading(false)
                @name.editInPlace("close", data)
                @el.attr("data-folder-id", data)
                @id = @el.attr("data-folder-id")
            error: (err) ->
                @loading(false)
                $.error("Error: " + err)
    deleteCompleteTasks: (e) =>
        e.preventDefault()
        $.each @tasks, (i, item) ->
            item.deleteTask() if item.el.find(".done:checked").length > 0
            true
        false
    deleteAllTasks: (e) =>
        e.preventDefault()
        $.each @tasks, (i, item)->
            item.deleteTask()
            true
        false
    deleteFolder: (e) =>
        e.preventDefault()
        @el.remove()
        false
    toggleAll: (e) =>
        val = $(e.target).is(":checked")
        $.each @tasks, (i, item) ->
            item.toggle(val)
            true
    refreshCount: =>
        count = @tasks.filter((item)->
            item.el.find(".done:checked").length == 0
        ).length
        @counter.text(count)
    deleteTask: (task) =>
        @tasks = _.without @tasks, tasks
        @refreshCount()
    loading: (display) ->
        if (display)
            @el.find("header .options").hide()
            @el.find("header .loader").show()
        else
            @el.find("header .options").show()
            @el.find("header .loader").hide()

# ------------------------------------- TASK ITEM
class TaskItem extends Backbone.View
    events:
        "change .done"          : "onToggle"
        "click .deleteTask"     : "deleteTask"
        "dblclick h4"           : "editTask"
    initialize: (options) ->
        @check = @el.find(".done")
        @id = @el.attr("data-task-id")
        @folder = options.folder
    deleteTask: (e) =>
        e.preventDefault() if e?
        @loading(false)
        r = jsRoutes.controllers.Tasks.delete(@id)
        $.ajax
            url: r.url
            type: r.type
            context: this
            data:
                name: name
            success: (data) ->
                @loading(false)
                @el.remove()
                @trigger("delete", @)
            error: (err) ->
                @loading(false)
                $.error("Error: " + err)
        false
    editTask: (e) =>
        e.preventDefault()
        # TODO
        alert "not implemented yet."
        false
    toggle: (val) =>
        @loading(true)
        r = jsRoutes.controllers.Tasks.update(@id)
        $.ajax
            url: r.url
            type: r.type
            context: this
            data:
                done: val
            success: (data) ->
                @loading(false)
                @check.attr("checked",val)
                @trigger("change", @)
            error: (err) ->
                @loading(false)
                $.error("Error: " + err)
    onToggle: (e) =>
        e.preventDefault()
        val = @check.is(":checked")
        log val
        @toggle(val)
        false
    loading: (display) ->
        if (display)
            @el.find(".delete").hide()
            @el.find(".loader").show()
        else
            @el.find(".delete").show()
            @el.find(".loader").hide()

# ------------------------------------- INIT APP
$ -> # document is ready!

    app = new AppRouter()
    drawer = new Drawer el: $("#projects")

    Backbone.history.start
        pushHistory: true


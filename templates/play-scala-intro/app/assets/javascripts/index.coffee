$ ->
  $.get "/persons", (persons) ->
    $.each persons, (index, person) ->
      name = $("<div>").addClass("name").text person.name
      age = $("<div>").addClass("age").text person.age
      $("#persons").append $("<li>").append(name).append(age)
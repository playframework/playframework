$(function() {

  // Tabbed code samples

  $("dl").has("dd > pre").each(function() {
    var dl = $(this);
    dl.addClass("tabbed");
    dl.find("dt").each(function(i) {
      var dt = $(this);
      dt.html("<a href=\"#tab" + i + "\">" + dt.text() + "</a>");
    });
    dl.find("dd").hide();
    var current = dl.find("dt:first").addClass("current");
    var currentContent = current.next("dd").show();
    dl.css("height", current.height() + currentContent.height());
  });

  $("dl.tabbed dt a").click(function(e){
    e.preventDefault();
    var current = $(this).parent("dt");
    var dl = current.parent("dl");
    dl.find(".current").removeClass("current").next("dd").hide();
    current.addClass("current");
    var currentContent = current.next("dd").show();
    dl.css("height", current.height() + currentContent.height());
  });

});

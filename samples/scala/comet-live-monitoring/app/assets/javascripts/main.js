(function (app) {

    function create(elt) { return window.document.createElement(elt); }

    function SpeedOMeter (config) {
        this.maxVal = config.maxVal;
        this.unit = config.unit ? config.unit + " " : "";
        this.name = config.name;
        this.container = config.container;
        this.elt = create("div");
        this.elt.className = "monitor";

        var title = create("span");
        title.innerHTML = this.name;
        title.className = 'title';
        this.elt.appendChild(title);

        this.screenCurrent = create("span");
        this.screenCurrent.className = 'screen current';
        this.elt.appendChild(this.screenCurrent);

        this.screenMax = create("span");
        this.screenMax.className = 'screen max';
        this.screenMax.innerHTML = this.maxVal + this.unit;
        this.elt.appendChild(this.screenMax);

        this.needle = create("div");
        this.needle.className = "needle";
        this.elt.appendChild(this.needle);

        this.light = create("div");
        this.light.className = "green light";
        this.elt.appendChild(this.light);

        var wheel = create("div");
        wheel.className = "wheel";
        this.elt.appendChild(wheel);

        this.container.appendChild(this.elt);
    }

    SpeedOMeter.prototype.red = function () {
        this.light.className = "red light";
    };

    SpeedOMeter.prototype.green = function () {
        this.light.className = "red green";
    };

    SpeedOMeter.prototype.update = function (val) {
        Zanimo.transition(
            this.needle,
            "transform",
            "rotate(" + (val > this.maxVal ? 175 : val * 170 / this.maxVal) + "deg)",
            500,
            "ease-in"
        );
        this.screenCurrent.innerHTML = val + this.unit;
    }

    function init() {

        window.document.addEventListener('touchmove', function (evt) {
            evt.preventDefault();
        }, false);

        app.rps = new SpeedOMeter({
            name : "RPS",
            maxVal : 40000,
            container : window.document.body
        });

        app.memory = new SpeedOMeter({
            name : "MEMORY",
            maxVal : app.totalMemory,
            unit : "MB",
            container : window.document.body
        });

        app.cpu = new SpeedOMeter({
            name : "CPU",
            maxVal : 100,
            unit : "%",
            container : window.document.body
        });

        var button = create("button");
        button.className = "gc";
        button.innerHTML = "GARBAGE COLLECT";

        button.addEventListener(
            button.ontouchstart === null ? "touchstart" : "click",
            function (evt){
                evt.target.className += " touch";
                var xhr = new XMLHttpRequest();
                xhr.open("POST", "/gc!", true);
                xhr.onreadystatechange = function (){
                    if(xhr.readyState == 4) {
                        evt.target.className = "gc";
                        xhr.status == 200 ? console.log(xhr.responseText) : console.log(xhr.status);
                    }
                };
                xhr.send();
            },
            false
        );

        window.document.body.appendChild(button);

        var iframe = create("iframe");
        iframe.src = "/monitoring";
        iframe.style.display = "none";

        window.message = function (msg) {
            var d = msg.split(":");
            app.lastCall = (new Date()).getTime();
            if (d.length == 2) {
                app[d[1]].update(d[0]);
            }
        }

        setTimeout(function () {
            app.lastCall = (new Date()).getTime();
            window.document.body.appendChild(iframe);
        }, 100);

        setInterval(function () {
            if ((new Date()).getTime() - app.lastCall > 5000) {
                app.rps.red();
                app.memory.red();
                app.cpu.red();
            }
        },300);
    }

    window.document.addEventListener("DOMContentLoaded", init, false);

})(window.App);

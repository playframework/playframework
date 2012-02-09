(function (app) {

    function $(elt) { return document.createElement(elt); }

    function SpeedOMeter (config) {
        this.maxVal = config.maxVal;
        this.unit = config.unit ? config.unit + " " : "";
        this.name = config.name;
        this.container = config.container;
        this.elt = $("div");
        this.elt.className = "monitor";

        var title = $("span");
        title.innerText = this.name;
        title.className = 'title';
        this.elt.appendChild(title);

        this.screenCurrent = $("span");
        this.screenCurrent.className = 'screen current';
        this.screenCurrent.innerText = "current";
        this.elt.appendChild(this.screenCurrent);

        this.screenMax = $("span");
        this.screenMax.className = 'screen max';
        this.screenMax.innerText = this.maxVal + this.unit;
        this.elt.appendChild(this.screenMax);

        this.needle = $("div");
        this.needle.className = "needle";
        this.elt.appendChild(this.needle);

        this.light = $("div");
        this.light.className = "green light";
        this.elt.appendChild(this.light);

        var wheel = $("div");
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
        if (val > this.maxVal) {
            this.maxVal = val;
            this.screenMax.innerText = val + this.unit;
        }
        Zanimo.transition(
            this.needle,
            "transform",
            "rotate(" + (val * 170 / this.maxVal) + "deg)",
            500,
            "ease-in"
        );
        this.screenCurrent.innerText = val + this.unit;
    }

    function init() {

        document.addEventListener('touchmove', function (evt) {
            evt.preventDefault();
        }, false);

        app.rps = new SpeedOMeter({
            name : "RPS",
            maxVal : 40000,
            container : document.body
        });

        app.memory = new SpeedOMeter({
            name : "MEMORY",
            maxVal : app.totalMemory,
            unit : "MB",
            container : document.body
        });

        app.cpu = new SpeedOMeter({
            name : "CPU",
            maxVal : 100,
            unit : "%",
            container : document.body
        });

        var button = $("button");
        button.className = "gc";
        button.innerText = "GARBAGE RESET";

        button.addEventListener("click", function (evt){
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
        }, false);

        document.body.appendChild(button);

        var iframe = $("iframe");
        iframe.src = "/speed-meter";
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

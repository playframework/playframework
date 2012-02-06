(function (app) {

    function SpeedOMeter (config) {
        try {
            this.maxVal = config.maxVal || 40000;
            this.name = config.name;
            this.container = config.container;
            this.elt = document.createElement("div");
            this.elt.className = "monitor";

            var title = document.createElement("span");
            title.innerText = this.name;
            title.className = 'title';
            this.elt.appendChild(title);

            this.screenCurrent = document.createElement("span");
            this.screenCurrent.className = 'screen current';
            this.screenCurrent.innerText = "current";
            this.elt.appendChild(this.screenCurrent);

            this.screenMax = document.createElement("span");
            this.screenMax.className = 'screen max';
            this.screenMax.innerText = "max";
            this.elt.appendChild(this.screenMax);

            this.needle = document.createElement("div");
            this.needle.className = "needle";
            this.elt.appendChild(this.needle);

            this.light = document.createElement("div");
            this.light.className = "green light";
            this.elt.appendChild(this.light);

            var wheel = document.createElement("div");
            wheel.className = "wheel";
            this.elt.appendChild(wheel);
        }
        catch(err) {
        }
    }

    SpeedOMeter.prototype.red = function () {
        this.light.className = "red light";
    };

    SpeedOMeter.prototype.green = function () {
        this.light.className = "red green";
    };

    SpeedOMeter.prototype.start = function () {
        var that = this;
        try {
            that.container.appendChild(that.elt);
            setTimeout(function () {
                Zanimo.transition(that.needle, "transform", "rotate(-90deg)", 1000, "ease-in");
            }, 100);
        }
        catch(err) {
        }
    }

    SpeedOMeter.prototype.change = function (angle) {
        var that = this;
        try {
            Zanimo.transition(that.needle, "transform", "rotate(" + (angle - 90) + "deg)", 1000, "ease-in");
        }
        catch(err) {
        }
    }

    function init() {

        document.addEventListener('touchmove', function (evt) {
            evt.preventDefault();
        }, false);

        app.rps = new SpeedOMeter({
                name : "RPS",
                container : document.body
        });
        app.memory = new SpeedOMeter({
                name : "MEM",
                container : document.body
        });
        app.cpu = new SpeedOMeter({
                name : "CPU",
                container : document.body
        });

        app.rps.start();
        app.memory.start();
        app.cpu.start();

        var button = document.createElement("button");
        button.className = "gc";
        button.innerText = "GARBAGE RESET";

        button.addEventListener("click", function () {
            var xhr = new XMLHttpRequest();
            xhr.open("POST", "/gc!", true);
            xhr.onreadystatechange = function () {
                if(xhr.readyState == 4) {
                    if (xhr.status == 200) {
                        console.log(xhr.responseText);
                    }
                    else {
                        console.log(xhr.status);
                    }
                }
            };

            xhr.send();
        }, false);

        document.body.appendChild(button);

        var iframe = document.createElement("iframe");
        iframe.src = "/speed-meter";
        iframe.style.display = "none";

        app.data = {
            cpu: {
                max : 0,
                current :0
            },
            memory : {
                max : 0,
                current :0
            },
            rps : {
                max : 0,
                current :0
            }
        };

        function update (item) {
            try {
                app.lastCall = (new Date()).getTime();
                var v = app.data[item].max == 0 ? 0 : app.data[item].current * 180 / app.data[item].max;
                app[item].change(v > 180 ? 180 : v);
                setTimeout(function () {
                    app[item].screenCurrent.innerHTML = app.data[item].current;
                    app[item].screenMax.innerHTML = app.data[item].max;
                }, 100);

            } catch(err) {
                console.log(err);
            }
        }

        window.message = function (msg) {
            var d = msg.split(":");
            app.data[d[1]].current = d[0];
            if (d[0] >= app.data[d[1]].max) {
                app.data[d[1]].max = d[0];
            }
            update(d[1]);
        }

        setTimeout(function () {
            app.lastCall = (new Date()).getTime();
            window.document.body.appendChild(iframe);
        }, 100);

        setInterval(function () {
            if ((new Date()).getTime() - app.lastCall > 1000) {
                app.rps.red();
                app.memory.red();
                app.cpu.red();
            }
        },300);
    }

    window.document.addEventListener("DOMContentLoaded", init, false);
})(App = {});

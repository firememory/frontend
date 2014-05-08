var app = angular.module('coinport.depth', ['ui.bootstrap', 'ngResource', 'navbar']);

app.controller('DepthCtrl', function ($scope, $http, $window) {
    $scope.market = $window.location.pathname.replace("/depth/", "");

    var accumulateDepth = function(items) {
        for(var i = 1; i < items.length; i++) {
           items[i].amount = items[i].amount + items[i - 1].amount;
        }
    };

    $http.get('/api/' + $scope.market + '/depth', {params: {depth: 30}})
        .success(function(data, status, headers, config) {
            $scope.depth = data.data;
            // accumulate depth
            var bids = data.data.bids.slice(0);
            var asks = data.data.asks.slice(0);
            accumulateDepth(bids);
            accumulateDepth(asks);
            $scope.drawGraph(bids, asks);
    });

    $scope.drawGraph = function(data1, data2) {
        var margin = {top: 20, right: 20, bottom: 30, left: 50},
            width = 1024 - margin.left - margin.right,
            height = 300 - margin.top - margin.bottom;

        var x = d3.scale.linear()
            .range([0, width]);

        var y = d3.scale.linear()
            .range([height, 0]);

        var xAxis = d3.svg.axis()
            .scale(x)
            .orient("bottom")
            .tickSize(-height, 0, 0);

        var yAxis = d3.svg.axis()
            .scale(y)
            .orient("left")
            .tickSize(-width, 0, 0);

        var area = d3.svg.area()
            .x(function(d) { return x(d.price); })
            .y0(height)
            .y1(function(d) { return y(d.amount); });

        var line = d3.svg.line()
            .x(function(d) { return x(d.price); })
            .y(function(d) { return y(d.amount); });

        var svg = d3.select("#depth-graph").append("svg")
            .attr("width", width + margin.left + margin.right)
            .attr("height", height + margin.top + margin.bottom)
          .append("g")
            .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

        var data = data1.reverse().concat(data2);

        x.domain(d3.extent(data, function(d) { return d.price; }));
        y.domain([0, d3.max(data, function(d) { return d.amount; })]);

        svg.append("path")
          .datum(data1.reverse())
          .attr("class", "area-buy")
          .attr("d", area);

        svg.append("path")
          .datum(data2)
          .attr("class", "area-sell")
          .attr("d", area);

        svg.append("path")
          .datum(data1.reverse())
          .attr("class", "line-buy")
          .attr("d", line);

        svg.append("path")
          .datum(data2)
          .attr("class", "line-sell")
          .attr("d", line);

        svg.append("g")
            .attr("class", "x axis")
            .attr("transform", "translate(0," + height + ")")
            .call(xAxis);

        svg.append("g")
            .attr("class", "y axis")
            .call(yAxis)
          .append("text")
            .attr("transform", "rotate(-90)")
            .attr("y", 6)
            .attr("dy", ".71em")
            .style("text-anchor", "end")
            .text("Amount");
    };
});
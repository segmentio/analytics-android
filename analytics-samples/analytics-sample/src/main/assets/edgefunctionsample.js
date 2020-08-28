function changeTestValue(event) {
    event.context.test = 2;
    return event;
}
function addValue(event) {
    event.context.cats = "gross";
    return event;
}
function addDogValue(event) {
    event.context.dogs = "tha bomb";
    return event;
}
function addMyObject(event) {
    event.context.myObject = {
        booya: 1,
        picard: "<facepalm>"
    };
    return event;
}
function dropEvent(event) {
    return null;
}
const sourceMiddleware = [
    changeTestValue,
    addValue,
];
const destinationMiddleware = {
    "Segment.io": [
        addMyObject,
        addDogValue
    ],
    "appboy": [
        dropEvent
    ]
};
console.log("loaded.")
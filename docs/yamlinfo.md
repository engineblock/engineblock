~~~ mermaid.js visual
file LR
subfile simple
sd1[statement definition] --> s1[statement]
sd1 --> b1[data bindings]
end

subfile multiple
sl2[statements list] --> sd2
sl2 --> sd3
sd2[statement definition] --> s2[statement]
sd2 --> b2[data bindings]
sd3[statement definition] --> s3[statement]
sd3 --> b3[data bindings]
end

subfile shared bindings
sl3 -->|owns| slb3[data bindings]
sl3[statements list] --> sd4
sd4[statement definition] --> s4[statement]
sd4 -->|uses| slb3
sl3 --> sd6
sd6[statement definition] -->|uses| slb3
sd6 --> s6[statement]
sl3 --> sd5[statement definition]
sd5--> s5[statement]
sd5 --> b5[data bindings]
end
~~~

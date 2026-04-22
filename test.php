<?php
$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, "https://generativelanguage.googleapis.com/v1beta/models?key=AIzaSyBJqh1JqP5aQ2XN19VY4nPpUUoitJsVWcQ");
curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
$response = json_decode(curl_exec($ch), true);
foreach($response['models'] as $m) echo $m['name'] . "\n";
curl_close($ch);

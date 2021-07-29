import {useContext, useEffect, useState} from "react";
import axios from "axios";
import {useHistory} from "react-router-dom";
//import google_logo from "../assets/btn_google_light_normal_ios.svg"
import AuthContext from "../context/AuthContext";
import React from "react";
import {StyleSheet, Button, Text, TextInput, View} from "react-native";
import Styles from "../Styles";
import GoogleLoginButton from "../components/GoogleLoginButton";

export default function LoginPage() {
    const [email, setEmail] = useState()
    const [password, setPassword] = useState();

    const classes = Styles()
    const [error, setError] = useState()
    const {login} = useContext(AuthContext)
    const history = useHistory();


    const handleSignIn = () => {
        login({email: email, password: password})
            .catch((error) => {
                setError(error.response?.data.message)
            })
    }

    const handleRegistrationClick = () => {
        history.push("https://photohunter.herokuapp.com/registration")
    }

    return (
        <View style={classes.card}>
            <Text style={classes.page_title}>
                Login
            </Text>
            {error && <Text style={classes.error}>{error}</Text>}
            <View style={classes.content}>
                <TextInput style={[classes.input, classes.shadow]}
                           value={email}
                           onChangeText={setEmail}
                           placeholder={"Email"}
                           keyboardType={"email-address"}/>
                <TextInput style={[classes.input, classes.shadow]}
                           value={password}
                           onChangeText={setPassword}
                           placeholder={"Password"}
                           secureTextEntry={true}/>
                <Button
                    title={"Sign In"}
                    onPress={handleSignIn}/>

                <Text style={classes.forgot} onPress={() => {
                    history.push("/forgot")
                }}>
                    Forgot your Password?
                </Text>

                <Button title={"Registration"} style={classes.item} onPress={handleRegistrationClick}/>

                <GoogleLoginButton/>
            </View>

        </View>
    )
};

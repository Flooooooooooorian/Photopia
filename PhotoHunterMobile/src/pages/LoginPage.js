import React, { useContext, useState } from 'react'
import { useHistory } from 'react-router-dom'
import AuthContext from '../context/AuthContext'
import { Text, View } from 'react-native'
import Styles from '../Styles'
import FormTextInput from '../components/FormTextInput'
import CustomButton from '../components/CustomButton'

export default function LoginPage() {
  const [email, setEmail] = useState()
  const [password, setPassword] = useState()

  const classes = Styles()
  const [error, setError] = useState()
  const { login } = useContext(AuthContext)
  const history = useHistory()

  const handleSignIn = () => {
    login({ email: email, password: password }).catch(error => {
      setError(error.response?.data.message)
    })
  }

  const handleRegistrationClick = () => {
    history.push('/registration')
  }

  return (
    <View style={classes.card}>
      <Text style={classes.page_title}>Login</Text>
      {error && <Text style={classes.error}>{error}</Text>}
      <View style={classes.content}>
        <FormTextInput
          titleText={'Email'}
          autoCorrect={false}
          placeholder={'Email'}
          value={email}
          onChangeText={setEmail}
          keyboardType={'email-address'}
        />
        <FormTextInput
          titleText={'Password'}
          autoCorrect={false}
          placeholder={'Password'}
          value={password}
          onChangeText={setPassword}
          secureTextEntry={true}
        />
        <CustomButton onPress={handleSignIn}>
          <Text style={{ textAlign: 'center', color: '#ffffff' }}>Sign In</Text>
        </CustomButton>

        <Text
          style={classes.forgot}
          onPress={() => {
            history.push('/forgot')
          }}
        >
          Forgot your Password?
        </Text>

        <CustomButton onPress={handleRegistrationClick}>
          <Text style={{ textAlign: 'center', color: '#ffffff' }}>
            Registration
          </Text>
        </CustomButton>

        {/*<GoogleLoginButton/>*/}
      </View>
    </View>
  )
}

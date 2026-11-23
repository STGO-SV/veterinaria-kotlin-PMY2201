package cl.duoc.veterinariaapp.utilidades

fun pesos(monto: Long): String = "$" + monto.toString().reversed().chunked(3).joinToString(".").reversed() + " CLP"

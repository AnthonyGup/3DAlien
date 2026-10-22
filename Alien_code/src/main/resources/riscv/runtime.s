# =============================================================================
#  runtime.s  --  Runtime de St4ck Over-Pig  (RISC-V RV32IMFD, RARS 1.6)
# =============================================================================
#  CONTRATO CON EL CODIGO GENERADO
#    s0 = &Stack   s1 = &Heap   s2 = &Str   (constantes, el runtime NO las toca)
#    s3 = P        s4 = H       s5 = S      (enteros; solo s5 lo cambian las rutinas
#                                            que reservan texto: rt_conc, rt_strd,
#                                            rt_strn, rt_strc)
#    Las rutinas pueden pisar t0-t6, a0-a7, ft0-ft11, fa0-fa7 y ra.
#    Argumentos: enteros en a0/a1, doubles en fa0. Resultado: a0 (entero) o fa0.
#    Un "indice de Str" es un entero: la cadena vive en s2 + indice (terminada en 0).
#
#  Entrada: SOLO se usa el ecall 8 (leer linea). Los ecalls 5, 7 y 12 de RARS son
#  por linea y abortan con el fin de entrada; aqui se emula scanf encima de ecall 8.
#  RARS no distingue una linea vacia del fin de entrada; ver rt_getc.
# =============================================================================

.data
.align 3
rt_c_ten:      .double 10.0
rt_c_1e5:      .double 100000.0
rt_c_half:     .double 0.5
rt_c_eps:      .double 0.001
rt_pow10tab:   .double 1e1, 1e2, 1e4, 1e8, 1e16, 1e32, 1e64, 1e128, 1e256
rt_scr:        .space 8
rt_digs:       .space 8
rt_numbuf:     .space 64
rt_inbuf:      .space 1032
rt_inpos:      .word 0
rt_inlen:      .word 0
rt_ineof:      .word 0
rt_blanks:     .word 0
rt_s_true:     .asciz "true"
rt_s_false:    .asciz "false"
rt_s_errdisp:  .asciz "Error: despacho virtual invalido\n"
rt_s_errnull:  .asciz "Error: referencia nula\n"
.align 3

.text
# ----------------------------------------------------------------------------
#  SALIDA
# ----------------------------------------------------------------------------
rt_print_int:                 # a0 = entero
    li   a7, 1
    ecall
    ret

rt_print_char:                # a0 = codigo de caracter
    li   a7, 11
    ecall
    ret

rt_print_cstr:                # a0 = DIRECCION de una cadena en memoria
    li   a7, 4
    ecall
    ret

rt_print_str:                 # a0 = INDICE en Str
    add  a0, a0, s2
    li   a7, 4
    ecall
    ret

rt_print_bool:                # fa0 = double; distinto de 0 -> "true"
    fcvt.d.w ft0, zero
    feq.d t0, fa0, ft0
    la   a0, rt_s_true
    beqz t0, rt_pb_go
    la   a0, rt_s_false
rt_pb_go:
    li   a7, 4
    ecall
    ret

rt_print_ptr:                 # a0 = referencia (%p); imprime 0x000000NN
    li   a7, 34
    ecall
    ret

rt_print_g:                   # fa0 = double, formato %g de C
    addi sp, sp, -16
    sw   ra, 12(sp)
    la   a0, rt_numbuf
    jal  ra, rt_fmt_g
    la   a0, rt_numbuf
    li   a7, 4
    ecall
    lw   ra, 12(sp)
    addi sp, sp, 16
    ret

rt_exit:
    li   a7, 10
    ecall

rt_error_despacho:            # clase sin metodo candidato en una llamada virtual
    la   a0, rt_s_errdisp
    li   a7, 4
    ecall
    li   a0, 1
    li   a7, 93
    ecall

rt_error_nulo:                # acceso a referencia nula
    la   a0, rt_s_errnull
    li   a7, 4
    ecall
    li   a0, 1
    li   a7, 93
    ecall

# ----------------------------------------------------------------------------
#  CADENAS
# ----------------------------------------------------------------------------
rt_strlen:                    # a0 = direccion -> a0 = longitud
    mv   t0, a0
rt_strlen_l:
    lb   t1, 0(t0)
    beqz t1, rt_strlen_e
    addi t0, t0, 1
    j    rt_strlen_l
rt_strlen_e:
    sub  a0, t0, a0
    ret

rt_strcpy:                    # a0 = destino, a1 = origen (copia tambien el 0)
rt_strcpy_l:
    lb   t1, 0(a1)
    sb   t1, 0(a0)
    addi a0, a0, 1
    addi a1, a1, 1
    bnez t1, rt_strcpy_l
    ret

rt_putc:                      # privado de rt_fmt_g: escribe a0 en (s6++)
    sb   a0, 0(s6)
    addi s6, s6, 1
    ret

rt_itoa:                      # a0 = destino (direccion), a1 = entero -> a0 = longitud
    mv   t0, a0
    mv   t6, a0
    bgez a1, rt_itoa_p
    li   t1, 45
    sb   t1, 0(t0)
    addi t0, t0, 1
    neg  a1, a1               # se trata como sin signo (sirve para INT_MIN)
rt_itoa_p:
    mv   t2, a1
    li   t3, 0
    li   t4, 10
rt_itoa_c:
    addi t3, t3, 1
    divu t2, t2, t4
    bnez t2, rt_itoa_c
    mv   a2, t3               # cantidad de digitos
    add  t5, t0, t3
    sb   zero, 0(t5)
rt_itoa_f:
    addi t5, t5, -1
    remu t1, a1, t4
    addi t1, t1, 48
    sb   t1, 0(t5)
    divu a1, a1, t4
    addi t3, t3, -1
    bnez t3, rt_itoa_f
    sub  a0, t0, t6
    add  a0, a0, a2
    ret

rt_strd:                      # a0 = entero -> a0 = indice en Str (sprintf "%d")
    addi sp, sp, -16
    sw   ra, 12(sp)
    sw   s5, 8(sp)
    mv   a1, a0
    add  a0, s2, s5
    jal  ra, rt_itoa
    addi a0, a0, 1
    add  s5, s5, a0
    lw   a0, 8(sp)
    lw   ra, 12(sp)
    addi sp, sp, 16
    ret

rt_strn:                      # fa0 = double -> a0 = indice en Str (sprintf "%g")
    addi sp, sp, -16
    sw   ra, 12(sp)
    sw   s5, 8(sp)
    add  a0, s2, s5
    jal  ra, rt_fmt_g
    addi a0, a0, 1
    add  s5, s5, a0
    lw   a0, 8(sp)
    lw   ra, 12(sp)
    addi sp, sp, 16
    ret

rt_strc:                      # a0 = caracter -> a0 = indice en Str
    add  t0, s2, s5
    sb   a0, 0(t0)
    sb   zero, 1(t0)
    mv   a0, s5
    addi s5, s5, 2
    ret

rt_conc:                      # a0 = indice A, a1 = indice B -> a0 = indice resultado
    mv   t0, s5
    add  t1, s2, s5
    add  t2, s2, a0
rt_conc_a:
    lb   t3, 0(t2)
    beqz t3, rt_conc_b0
    sb   t3, 0(t1)
    addi t1, t1, 1
    addi t2, t2, 1
    j    rt_conc_a
rt_conc_b0:
    add  t2, s2, a1
rt_conc_b:
    lb   t3, 0(t2)
    sb   t3, 0(t1)
    beqz t3, rt_conc_e
    addi t1, t1, 1
    addi t2, t2, 1
    j    rt_conc_b
rt_conc_e:
    addi t1, t1, 1
    sub  s5, t1, s2
    mv   a0, t0
    ret

# ----------------------------------------------------------------------------
#  POTENCIAS DE 10 y FORMATO %g
# ----------------------------------------------------------------------------
rt_pow10:                     # a0 = n (0..511) -> fa0 = 10^n
    la   t0, rt_pow10tab
    li   t1, 1
    fcvt.d.w fa0, t1
rt_pw_l:
    beqz a0, rt_pw_e
    andi t2, a0, 1
    beqz t2, rt_pw_s
    fld  ft0, 0(t0)
    fmul.d fa0, fa0, ft0
rt_pw_s:
    addi t0, t0, 8
    srli a0, a0, 1
    j    rt_pw_l
rt_pw_e:
    ret

rt_fmt_g:                     # a0 = destino, fa0 = double -> a0 = longitud (escribe 0 final)
    addi sp, sp, -32
    sw   ra, 28(sp)
    sw   s6, 24(sp)
    sw   s7, 20(sp)
    sw   s8, 16(sp)
    sw   s9, 12(sp)
    mv   s6, a0               # s6 = puntero de escritura
    mv   s8, a0               # s8 = inicio
    feq.d t0, fa0, fa0
    bnez t0, fg_notnan
    li   a0, 110              # "nan"
    jal  ra, rt_putc
    li   a0, 97
    jal  ra, rt_putc
    li   a0, 110
    jal  ra, rt_putc
    j    fg_end
fg_notnan:
    la   t0, rt_scr
    fsd  fa0, 0(t0)
    lw   t1, 4(t0)
    srli t2, t1, 31           # bit de signo
    beqz t2, fg_pos
    li   a0, 45
    jal  ra, rt_putc
fg_pos:
    fabs.d ft1, fa0
    fcvt.d.w ft0, zero
    feq.d t0, ft1, ft0
    beqz t0, fg_nonzero
    li   a0, 48               # "0"
    jal  ra, rt_putc
    j    fg_end
fg_nonzero:
    la   t0, rt_scr
    fsd  ft1, 0(t0)
    lw   t1, 4(t0)
    srli t2, t1, 20
    andi t2, t2, 2047
    li   t3, 2047
    bne  t2, t3, fg_finite
    li   a0, 105              # "inf"
    jal  ra, rt_putc
    li   a0, 110
    jal  ra, rt_putc
    li   a0, 102
    jal  ra, rt_putc
    j    fg_end
fg_finite:
    fmv.d ft6, ft1            # ft6 = |x| original (para el redondeo exacto)
    addi t2, t2, -1023
    li   t3, 30103
    mul  t2, t2, t3
    li   t3, 100000
    div  s7, t2, t3           # s7 = exponente decimal estimado
    bltz s7, fg_neg
    mv   a0, s7
    jal  ra, rt_pow10
    fdiv.d ft1, ft1, fa0
    j    fg_adj
fg_neg:
    neg  a0, s7
    jal  ra, rt_pow10
    fmul.d ft1, ft1, fa0
fg_adj:
    la   t0, rt_c_ten
    fld  ft3, 0(t0)
fg_adj_hi:
    fle.d t1, ft3, ft1        # 10 <= y
    beqz t1, fg_adj_lo
    fdiv.d ft1, ft1, ft3
    addi s7, s7, 1
    j    fg_adj_hi
fg_adj_lo:
    li   t1, 1
    fcvt.d.w ft4, t1
    flt.d t1, ft1, ft4        # y < 1
    beqz t1, fg_round
    fmul.d ft1, ft1, ft3
    addi s7, s7, -1
    j    fg_adj_lo
fg_round:
    la   t0, rt_c_1e5
    fld  ft5, 0(t0)
    fmul.d ft1, ft1, ft5      # S aprox = y * 1e5
    fcvt.w.d t3, ft1, rdn     # piso
    fcvt.d.w ft7, t3
    fsub.d ft2, ft1, ft7      # parte fraccionaria aprox
    la   t0, rt_c_half
    fld  ft8, 0(t0)
    fsub.d ft2, ft2, ft8
    fabs.d ft2, ft2
    la   t0, rt_c_eps
    fld  ft9, 0(t0)
    flt.d t1, ft2, ft9        # |fr - 0.5| < eps ?  (cerca de un empate)
    bnez t1, fg_exact
    fcvt.w.d t3, ft1          # lejos de un empate: al mas cercano
    j    fg_rchk
fg_exact:                     # comparacion EXACTA de S contra t = piso + 0.5
    fadd.d ft7, ft7, ft8      # t
    fcvt.d.w ft4, zero
    li   t1, 5
    sub  t2, t1, s7           # k = 5 - e10
    bltz t2, fg_exb
    li   t1, 22
    bgt  t2, t1, fg_exfall
    mv   a0, t2
    jal  ra, rt_pow10         # fa0 = 10^k (exacto)
    fmul.d ft2, ft6, fa0      # hi = x * s
    fmsub.d ft3, ft6, fa0, ft2  # lo = x*s - hi (exacto)
    fsub.d ft2, ft2, ft7      # hi - t (exacto)
    feq.d t1, ft2, ft4
    beqz t1, fg_decide
    fmv.d ft2, ft3            # si hi == t, manda el signo de lo
    j    fg_decide
fg_exb:
    neg  t2, t2               # k = e10 - 5
    li   t1, 22
    bgt  t2, t1, fg_exfall
    mv   a0, t2
    jal  ra, rt_pow10         # fa0 = s' = 10^k (exacto)
    fmul.d ft2, ft7, fa0      # hi2 = t * s'
    fmsub.d ft3, ft7, fa0, ft2  # lo2
    fsub.d ft2, ft6, ft2      # a = x - hi2 (exacto)
    fcvt.d.w ft4, zero
    feq.d t1, ft2, ft4
    beqz t1, fg_decide
    fneg.d ft2, ft3           # si a == 0, el signo es -lo2
fg_decide:                    # ft2 tiene el signo de (S - t)
    fcvt.d.w ft4, zero
    flt.d t1, ft4, ft2
    bnez t1, fg_up            # S > t: sube
    flt.d t1, ft2, ft4
    bnez t1, fg_rchk          # S < t: baja (queda el piso)
    andi t1, t3, 1            # empate: al par
    beqz t1, fg_rchk
fg_up:
    addi t3, t3, 1
    j    fg_rchk
fg_exfall:
    fcvt.w.d t3, ft1
fg_rchk:
    li   t4, 1000000
    blt  t3, t4, fg_dig
    li   t3, 100000
    addi s7, s7, 1
fg_dig:
    la   t5, rt_digs
    li   t4, 10
    li   t6, 5
fg_dloop:
    remu t1, t3, t4
    addi t1, t1, 48
    add  t2, t5, t6
    sb   t1, 0(t2)
    divu t3, t3, t4
    addi t6, t6, -1
    bgez t6, fg_dloop
    li   s9, 6                # s9 = cifras significativas sin ceros finales
fg_trim:
    li   t1, 1
    ble  s9, t1, fg_style
    add  t2, t5, s9
    lb   t1, -1(t2)
    li   t3, 48
    bne  t1, t3, fg_style
    addi s9, s9, -1
    j    fg_trim
fg_style:
    li   t1, -4
    blt  s7, t1, fg_sci
    li   t1, 6
    bge  s7, t1, fg_sci
    bltz s7, fg_fixneg
    li   a2, 0                # --- fijo, exponente 0..5: parte entera
fg_ip:
    blt  a2, s9, fg_ip_d
    li   a0, 48
    j    fg_ip_p
fg_ip_d:
    la   t5, rt_digs
    add  t2, t5, a2
    lbu  a0, 0(t2)
fg_ip_p:
    jal  ra, rt_putc
    addi a2, a2, 1
    ble  a2, s7, fg_ip
    addi t1, s7, 1
    ble  s9, t1, fg_end       # sin parte decimal
    li   a0, 46
    jal  ra, rt_putc
    addi a2, s7, 1
fg_fr:
    la   t5, rt_digs
    add  t2, t5, a2
    lbu  a0, 0(t2)
    jal  ra, rt_putc
    addi a2, a2, 1
    blt  a2, s9, fg_fr
    j    fg_end
fg_fixneg:                    # --- fijo, exponente -4..-1: 0.000ddd
    li   a0, 48
    jal  ra, rt_putc
    li   a0, 46
    jal  ra, rt_putc
    neg  a2, s7
    addi a2, a2, -1
fg_zl:
    blez a2, fg_zd
    li   a0, 48
    jal  ra, rt_putc
    addi a2, a2, -1
    j    fg_zl
fg_zd:
    li   a2, 0
fg_nd:
    la   t5, rt_digs
    add  t2, t5, a2
    lbu  a0, 0(t2)
    jal  ra, rt_putc
    addi a2, a2, 1
    blt  a2, s9, fg_nd
    j    fg_end
fg_sci:                       # --- cientifica: d.ddde+XX
    la   t5, rt_digs
    lbu  a0, 0(t5)
    jal  ra, rt_putc
    li   t1, 1
    ble  s9, t1, fg_sci_e
    li   a0, 46
    jal  ra, rt_putc
    li   a2, 1
fg_sd:
    la   t5, rt_digs
    add  t2, t5, a2
    lbu  a0, 0(t2)
    jal  ra, rt_putc
    addi a2, a2, 1
    blt  a2, s9, fg_sd
fg_sci_e:
    li   a0, 101
    jal  ra, rt_putc
    bltz s7, fg_en
    li   a0, 43
    jal  ra, rt_putc
    mv   a2, s7
    j    fg_ev
fg_en:
    li   a0, 45
    jal  ra, rt_putc
    neg  a2, s7
fg_ev:
    li   t1, 100
    blt  a2, t1, fg_e2d
    div  t2, a2, t1
    addi a0, t2, 48
    jal  ra, rt_putc
    li   t1, 100
    rem  a2, a2, t1
fg_e2d:
    li   t1, 10
    div  t2, a2, t1
    addi a0, t2, 48
    jal  ra, rt_putc
    li   t1, 10
    rem  t2, a2, t1
    addi a0, t2, 48
    jal  ra, rt_putc
fg_end:
    sb   zero, 0(s6)
    sub  a0, s6, s8
    lw   ra, 28(sp)
    lw   s6, 24(sp)
    lw   s7, 20(sp)
    lw   s8, 16(sp)
    lw   s9, 12(sp)
    addi sp, sp, 32
    ret

# ----------------------------------------------------------------------------
#  ENTRADA  (emulacion de scanf sobre el ecall 8)
# ----------------------------------------------------------------------------
# rt_getc: a0 = siguiente byte (0..255) o -1 si no hay mas entrada.
#   RARS agrega '\n' a la ultima linea si no lo tiene y devuelve "\n" tras el fin
#   de entrada, asi que NO se puede distinguir una linea vacia del fin.
#   Se considera fin de entrada cuando (a) llega una linea que empieza con 0x04
#   (RarsRunner la agrega al final del stdin) o (b) llegan 64 lineas vacias seguidas.
rt_getc:
    la   t0, rt_inpos
    lw   t1, 0(t0)
    la   t2, rt_inlen
    lw   t3, 0(t2)
    blt  t1, t3, gc_have
    la   t4, rt_ineof
    lw   t5, 0(t4)
    bnez t5, gc_eof
    la   a0, rt_inbuf
    li   a1, 1024
    li   a7, 8
    ecall
    la   t3, rt_inbuf
    mv   t4, t3
gc_len:
    lbu  t5, 0(t4)
    beqz t5, gc_lend
    addi t4, t4, 1
    j    gc_len
gc_lend:
    sub  t3, t4, t3           # longitud leida
    beqz t3, gc_seteof
    la   t2, rt_inlen
    sw   t3, 0(t2)
    la   t0, rt_inpos
    sw   zero, 0(t0)
    la   t6, rt_inbuf
    lbu  t5, 0(t6)
    li   t6, 4
    beq  t5, t6, gc_seteof    # centinela de fin de entrada
    li   t6, 10
    bne  t5, t6, gc_nonblank
    li   t6, 1
    bne  t3, t6, gc_nonblank
    la   t6, rt_blanks
    lw   t4, 0(t6)
    addi t4, t4, 1
    sw   t4, 0(t6)
    li   t5, 64
    bge  t4, t5, gc_seteof
    j    rt_getc
gc_nonblank:
    la   t6, rt_blanks
    sw   zero, 0(t6)
    j    rt_getc
gc_seteof:
    la   t4, rt_ineof
    li   t5, 1
    sw   t5, 0(t4)
    la   t2, rt_inlen
    sw   zero, 0(t2)
gc_eof:
    li   a0, -1
    ret
gc_have:
    la   t4, rt_inbuf
    add  t4, t4, t1
    lbu  a0, 0(t4)
    addi t1, t1, 1
    sw   t1, 0(t0)
    ret

rt_ungetc:                    # devuelve el ultimo byte leido (solo tras un rt_getc exitoso)
    la   t0, rt_inpos
    lw   t1, 0(t0)
    addi t1, t1, -1
    sw   t1, 0(t0)
    ret

rt_skipws:                    # salta blancos; a0 = primer byte util (sin consumir) o -1
    addi sp, sp, -16
    sw   ra, 12(sp)
rt_sw_l:
    jal  ra, rt_getc
    bltz a0, rt_sw_e
    li   t0, 32
    ble  a0, t0, rt_sw_l
    jal  ra, rt_ungetc
rt_sw_e:
    lw   ra, 12(sp)
    addi sp, sp, 16
    ret

rt_read_char:                 # scanf(" %c") -> a0 (0 si no hay entrada)
    addi sp, sp, -16
    sw   ra, 12(sp)
    jal  ra, rt_skipws
    bltz a0, rt_rc_z
    jal  ra, rt_getc
    j    rt_rc_e
rt_rc_z:
    li   a0, 0
rt_rc_e:
    lw   ra, 12(sp)
    addi sp, sp, 16
    ret

rt_read_int:                  # scanf("%d") -> a0 (0 si no hay numero)
    addi sp, sp, -32
    sw   ra, 28(sp)
    sw   s6, 24(sp)
    sw   s7, 20(sp)
    jal  ra, rt_skipws
    bltz a0, rt_ri_z
    li   s6, 0
    li   t0, 45
    beq  a0, t0, rt_ri_n
    li   t0, 43
    beq  a0, t0, rt_ri_p
    j    rt_ri_d
rt_ri_n:
    li   s6, 1
rt_ri_p:
    jal  ra, rt_getc
rt_ri_d:
    li   s7, 0
rt_ri_l:
    jal  ra, rt_getc
    bltz a0, rt_ri_e
    addi t0, a0, -48
    li   t1, 9
    bltu t1, t0, rt_ri_u
    li   t2, 10
    mul  s7, s7, t2
    add  s7, s7, t0
    j    rt_ri_l
rt_ri_u:
    jal  ra, rt_ungetc
rt_ri_e:
    mv   a0, s7
    beqz s6, rt_ri_r
    neg  a0, a0
    j    rt_ri_r
rt_ri_z:
    li   a0, 0
rt_ri_r:
    lw   ra, 28(sp)
    lw   s6, 24(sp)
    lw   s7, 20(sp)
    addi sp, sp, 32
    ret

rt_read_str:                  # scanf("%s", &Str[a0]): una palabra, escribe 0 final
    addi sp, sp, -16
    sw   ra, 12(sp)
    sw   s6, 8(sp)
    add  s6, s2, a0
    jal  ra, rt_skipws
    bltz a0, rt_rs_e
rt_rs_l:
    jal  ra, rt_getc
    bltz a0, rt_rs_e
    li   t0, 32
    ble  a0, t0, rt_rs_u
    sb   a0, 0(s6)
    addi s6, s6, 1
    j    rt_rs_l
rt_rs_u:
    jal  ra, rt_ungetc
rt_rs_e:
    sb   zero, 0(s6)
    lw   ra, 12(sp)
    lw   s6, 8(sp)
    addi sp, sp, 16
    ret

rt_read_double:               # scanf("%lf") -> fa0 (0.0 si no hay numero)
    addi sp, sp, -48
    sw   ra, 44(sp)
    sw   s6, 40(sp)
    sw   s7, 36(sp)
    sw   s8, 32(sp)
    sw   s9, 28(sp)
    sw   s10, 24(sp)
    li   s6, 0                # signo del numero
    li   s7, 0                # cantidad de decimales
    li   s8, 0                # exponente explicito
    li   s9, 0                # cifras vistas
    li   s10, 0               # signo del exponente
    fcvt.d.w ft8, zero        # mantisa
    la   t0, rt_c_ten
    fld  ft9, 0(t0)
    jal  ra, rt_skipws
    bltz a0, rd_zero
    li   t0, 45
    beq  a0, t0, rd_n
    li   t0, 43
    beq  a0, t0, rd_p
    j    rd_ip
rd_n:
    li   s6, 1
rd_p:
    jal  ra, rt_getc
rd_ip:
    jal  ra, rt_getc
    bltz a0, rd_fin
    addi t0, a0, -48
    li   t1, 9
    bltu t1, t0, rd_ip_end
    fmul.d ft8, ft8, ft9
    fcvt.d.w ft10, t0
    fadd.d ft8, ft8, ft10
    addi s9, s9, 1
    j    rd_ip
rd_ip_end:
    li   t1, 46
    bne  a0, t1, rd_chk
rd_fp:
    jal  ra, rt_getc
    bltz a0, rd_fin
    addi t0, a0, -48
    li   t1, 9
    bltu t1, t0, rd_chk
    fmul.d ft8, ft8, ft9
    fcvt.d.w ft10, t0
    fadd.d ft8, ft8, ft10
    addi s7, s7, 1
    addi s9, s9, 1
    j    rd_fp
rd_chk:
    li   t1, 101
    beq  a0, t1, rd_exp
    li   t1, 69
    beq  a0, t1, rd_exp
    jal  ra, rt_ungetc
    j    rd_fin
rd_exp:
    jal  ra, rt_getc
    bltz a0, rd_apply
    li   t1, 45
    beq  a0, t1, rd_en
    li   t1, 43
    beq  a0, t1, rd_ep
    j    rd_ed
rd_en:
    li   s10, 1
rd_ep:
    jal  ra, rt_getc
    bltz a0, rd_apply
rd_ed:
    addi t0, a0, -48
    li   t1, 9
    bltu t1, t0, rd_ed_u
    li   t2, 10
    mul  s8, s8, t2
    add  s8, s8, t0
    jal  ra, rt_getc
    bltz a0, rd_apply
    j    rd_ed
rd_ed_u:
    jal  ra, rt_ungetc
rd_apply:
    beqz s10, rd_fin
    neg  s8, s8
rd_fin:
    beqz s9, rd_zero
    sub  s8, s8, s7           # n = exponente - decimales
rd_bh:
    li   t1, 280
    ble  s8, t1, rd_bl
    li   a0, 280
    jal  ra, rt_pow10
    fmul.d ft8, ft8, fa0
    addi s8, s8, -280
    j    rd_bh
rd_bl:
    li   t1, -280
    bge  s8, t1, rd_mp
    li   a0, 280
    jal  ra, rt_pow10
    fdiv.d ft8, ft8, fa0
    addi s8, s8, 280
    j    rd_bl
rd_mp:
    bgez s8, rd_mul
    neg  a0, s8
    jal  ra, rt_pow10
    fdiv.d fa0, ft8, fa0
    j    rd_sign
rd_mul:
    mv   a0, s8
    jal  ra, rt_pow10
    fmul.d fa0, ft8, fa0
rd_sign:
    beqz s6, rd_ret
    fneg.d fa0, fa0
    j    rd_ret
rd_zero:
    fcvt.d.w fa0, zero
rd_ret:
    lw   ra, 44(sp)
    lw   s6, 40(sp)
    lw   s7, 36(sp)
    lw   s8, 32(sp)
    lw   s9, 28(sp)
    lw   s10, 24(sp)
    addi sp, sp, 48
    ret

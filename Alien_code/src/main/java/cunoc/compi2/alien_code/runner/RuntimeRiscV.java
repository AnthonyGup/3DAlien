package cunoc.compi2.alien_code.runner;

public final class RuntimeRiscV {
    private static final String RUNTIME =
            ".data\n" +
            ".align 3\n" +
            "rt_c_ten:      .double 10.0\n" +
            "rt_c_1e5:      .double 100000.0\n" +
            "rt_c_half:     .double 0.5\n" +
            "rt_c_eps:      .double 0.001\n" +
            "rt_pow10tab:   .double 1e1, 1e2, 1e4, 1e8, 1e16, 1e32, 1e64, 1e128, 1e256\n" +
            "rt_scr:        .space 8\n" +
            "rt_digs:       .space 8\n" +
            "rt_numbuf:     .space 64\n" +
            "rt_inbuf:      .space 1032\n" +
            "rt_inpos:      .word 0\n" +
            "rt_inlen:      .word 0\n" +
            "rt_ineof:      .word 0\n" +
            "rt_blanks:     .word 0\n" +
            "rt_s_true:     .asciz \"true\"\n" +
            "rt_s_false:    .asciz \"false\"\n" +
            "rt_s_errdisp:  .asciz \"Error: despacho virtual invalido\\n\"\n" +
            "rt_s_errnull:  .asciz \"Error: referencia nula\\n\"\n" +
            "rt_s_errstr:   .asciz \"Error: cadena nula\\n\"\n" +
            "rt_s_erridx:   .asciz \"Error: indice fuera de rango\\n\"\n" +
            "rt_s_errdiv:   .asciz \"Error: division entre cero\\n\"\n" +
            ".align 3\n" +
            ".text\n" +
            "rt_print_int:\n" +
            "    li   a7, 1\n" +
            "    ecall\n" +
            "    ret\n" +
            "rt_print_char:\n" +
            "    li   a7, 11\n" +
            "    ecall\n" +
            "    ret\n" +
            "rt_print_cstr:\n" +
            "rt_print_str:\n" +
            "    li   a7, 4\n" +
            "    ecall\n" +
            "    ret\n" +
            "rt_print_bool:\n" +
            "    mv   t0, a0\n" +
            "    la   a0, rt_s_true\n" +
            "    bnez t0, rt_pb_go\n" +
            "    la   a0, rt_s_false\n" +
            "rt_pb_go:\n" +
            "    li   a7, 4\n" +
            "    ecall\n" +
            "    ret\n" +
            "rt_print_ptr:\n" +
            "    li   a7, 34\n" +
            "    ecall\n" +
            "    ret\n" +
            "rt_print_g:\n" +
            "    addi sp, sp, -16\n" +
            "    sw   ra, 12(sp)\n" +
            "    la   a0, rt_numbuf\n" +
            "    jal  ra, rt_fmt_g\n" +
            "    la   a0, rt_numbuf\n" +
            "    li   a7, 4\n" +
            "    ecall\n" +
            "    lw   ra, 12(sp)\n" +
            "    addi sp, sp, 16\n" +
            "    ret\n" +
            "rt_exit:\n" +
            "    li   a7, 10\n" +
            "    ecall\n" +
            "rt_error_despacho:\n" +
            "    la   a0, rt_s_errdisp\n" +
            "    li   a7, 4\n" +
            "    ecall\n" +
            "    li   a0, 1\n" +
            "    li   a7, 93\n" +
            "    ecall\n" +
            "rt_error_nulo:\n" +
            "    la   a0, rt_s_errnull\n" +
            "    li   a7, 4\n" +
            "    ecall\n" +
            "    li   a0, 1\n" +
            "    li   a7, 93\n" +
            "    ecall\n" +
            "rt_error_indice:\n" +
            "    la   a0, rt_s_erridx\n" +
            "    li   a7, 4\n" +
            "    ecall\n" +
            "    li   a0, 1\n" +
            "    li   a7, 93\n" +
            "    ecall\n" +
            "rt_error_division:\n" +
            "    la   a0, rt_s_errdiv\n" +
            "    li   a7, 4\n" +
            "    ecall\n" +
            "    li   a0, 1\n" +
            "    li   a7, 93\n" +
            "    ecall\n" +
            "rt_strlen:\n" +
            "    mv   t0, a0\n" +
            "rt_strlen_l:\n" +
            "    lb   t1, 0(t0)\n" +
            "    beqz t1, rt_strlen_e\n" +
            "    addi t0, t0, 1\n" +
            "    j    rt_strlen_l\n" +
            "rt_strlen_e:\n" +
            "    sub  a0, t0, a0\n" +
            "    ret\n" +
            "rt_str_eq:\n" +
            "    beqz a0, rt_str_eq_left_null\n" +
            "    beqz a1, rt_str_eq_false\n" +
            "rt_str_eq_loop:\n" +
            "    lbu t0, 0(a0)\n" +
            "    lbu t1, 0(a1)\n" +
            "    bne t0, t1, rt_str_eq_false\n" +
            "    beqz t0, rt_str_eq_true\n" +
            "    addi a0, a0, 1\n" +
            "    addi a1, a1, 1\n" +
            "    j rt_str_eq_loop\n" +
            "rt_str_eq_left_null:\n" +
            "    seqz a0, a1\n" +
            "    ret\n" +
            "rt_str_eq_true:\n" +
            "    li a0, 1\n" +
            "    ret\n" +
            "rt_str_eq_false:\n" +
            "    li a0, 0\n" +
            "    ret\n" +
            "rt_strcpy:\n" +
            "rt_strcpy_l:\n" +
            "    lb   t1, 0(a1)\n" +
            "    sb   t1, 0(a0)\n" +
            "    addi a0, a0, 1\n" +
            "    addi a1, a1, 1\n" +
            "    bnez t1, rt_strcpy_l\n" +
            "    ret\n" +
            "rt_putc:\n" +
            "    sb   a0, 0(s6)\n" +
            "    addi s6, s6, 1\n" +
            "    ret\n" +
            "rt_itoa:\n" +
            "    mv   t0, a0\n" +
            "    mv   t6, a0\n" +
            "    bgez a1, rt_itoa_p\n" +
            "    li   t1, 45\n" +
            "    sb   t1, 0(t0)\n" +
            "    addi t0, t0, 1\n" +
            "    neg  a1, a1\n" +
            "rt_itoa_p:\n" +
            "    mv   t2, a1\n" +
            "    li   t3, 0\n" +
            "    li   t4, 10\n" +
            "rt_itoa_c:\n" +
            "    addi t3, t3, 1\n" +
            "    divu t2, t2, t4\n" +
            "    bnez t2, rt_itoa_c\n" +
            "    mv   a2, t3\n" +
            "    add  t5, t0, t3\n" +
            "    sb   zero, 0(t5)\n" +
            "rt_itoa_f:\n" +
            "    addi t5, t5, -1\n" +
            "    remu t1, a1, t4\n" +
            "    addi t1, t1, 48\n" +
            "    sb   t1, 0(t5)\n" +
            "    divu a1, a1, t4\n" +
            "    addi t3, t3, -1\n" +
            "    bnez t3, rt_itoa_f\n" +
            "    sub  a0, t0, t6\n" +
            "    add  a0, a0, a2\n" +
            "    ret\n" +
            "rt_strd:\n" +
            "    addi sp, sp, -16\n" +
            "    sw   ra, 12(sp)\n" +
            "    sw   s2, 8(sp)\n" +
            "    mv   a1, a0\n" +
            "    mv   a0, s2\n" +
            "    jal  ra, rt_itoa\n" +
            "    addi a0, a0, 1\n" +
            "    add  s2, s2, a0\n" +
            "    lw   a0, 8(sp)\n" +
            "    lw   ra, 12(sp)\n" +
            "    addi sp, sp, 16\n" +
            "    ret\n" +
            "rt_strn:\n" +
            "    addi sp, sp, -16\n" +
            "    sw   ra, 12(sp)\n" +
            "    sw   s2, 8(sp)\n" +
            "    mv   a0, s2\n" +
            "    jal  ra, rt_fmt_g\n" +
            "    addi a0, a0, 1\n" +
            "    add  s2, s2, a0\n" +
            "    lw   a0, 8(sp)\n" +
            "    lw   ra, 12(sp)\n" +
            "    addi sp, sp, 16\n" +
            "    ret\n" +
            "rt_strc:\n" +
            "    mv   t0, s2\n" +
            "    sb   a0, 0(t0)\n" +
            "    sb   zero, 1(t0)\n" +
            "    mv   a0, s2\n" +
            "    addi s2, s2, 2\n" +
            "    ret\n" +
            "rt_conc:\n" +
            "    mv   t0, s2\n" +
            "    mv   t1, s2\n" +
            "    mv   t2, a0\n" +
            "rt_conc_a:\n" +
            "    lb   t3, 0(t2)\n" +
            "    beqz t3, rt_conc_b0\n" +
            "    sb   t3, 0(t1)\n" +
            "    addi t1, t1, 1\n" +
            "    addi t2, t2, 1\n" +
            "    j    rt_conc_a\n" +
            "rt_conc_b0:\n" +
            "    mv   t2, a1\n" +
            "rt_conc_b:\n" +
            "    lb   t3, 0(t2)\n" +
            "    sb   t3, 0(t1)\n" +
            "    beqz t3, rt_conc_e\n" +
            "    addi t1, t1, 1\n" +
            "    addi t2, t2, 1\n" +
            "    j    rt_conc_b\n" +
            "rt_conc_e:\n" +
            "    addi t1, t1, 1\n" +
            "    mv   s2, t1\n" +
            "    mv   a0, t0\n" +
            "    ret\n" +
            "rt_pow10:\n" +
            "    la   t0, rt_pow10tab\n" +
            "    li   t1, 1\n" +
            "    fcvt.d.w fa0, t1\n" +
            "rt_pw_l:\n" +
            "    beqz a0, rt_pw_e\n" +
            "    andi t2, a0, 1\n" +
            "    beqz t2, rt_pw_s\n" +
            "    fld  ft0, 0(t0)\n" +
            "    fmul.d fa0, fa0, ft0\n" +
            "rt_pw_s:\n" +
            "    addi t0, t0, 8\n" +
            "    srli a0, a0, 1\n" +
            "    j    rt_pw_l\n" +
            "rt_pw_e:\n" +
            "    ret\n" +
            "rt_fmt_g:\n" +
            "    addi sp, sp, -32\n" +
            "    sw   ra, 28(sp)\n" +
            "    sw   s6, 24(sp)\n" +
            "    sw   s7, 20(sp)\n" +
            "    sw   s8, 16(sp)\n" +
            "    sw   s9, 12(sp)\n" +
            "    mv   s6, a0\n" +
            "    mv   s8, a0\n" +
            "    feq.d t0, fa0, fa0\n" +
            "    bnez t0, fg_notnan\n" +
            "    li   a0, 110\n" +
            "    jal  ra, rt_putc\n" +
            "    li   a0, 97\n" +
            "    jal  ra, rt_putc\n" +
            "    li   a0, 110\n" +
            "    jal  ra, rt_putc\n" +
            "    j    fg_end\n" +
            "fg_notnan:\n" +
            "    la   t0, rt_scr\n" +
            "    fsd  fa0, 0(t0)\n" +
            "    lw   t1, 4(t0)\n" +
            "    srli t2, t1, 31\n" +
            "    beqz t2, fg_pos\n" +
            "    li   a0, 45\n" +
            "    jal  ra, rt_putc\n" +
            "fg_pos:\n" +
            "    fabs.d ft1, fa0\n" +
            "    fcvt.d.w ft0, zero\n" +
            "    feq.d t0, ft1, ft0\n" +
            "    beqz t0, fg_nonzero\n" +
            "    li   a0, 48\n" +
            "    jal  ra, rt_putc\n" +
            "    j    fg_end\n" +
            "fg_nonzero:\n" +
            "    la   t0, rt_scr\n" +
            "    fsd  ft1, 0(t0)\n" +
            "    lw   t1, 4(t0)\n" +
            "    srli t2, t1, 20\n" +
            "    andi t2, t2, 2047\n" +
            "    li   t3, 2047\n" +
            "    bne  t2, t3, fg_finite\n" +
            "    li   a0, 105\n" +
            "    jal  ra, rt_putc\n" +
            "    li   a0, 110\n" +
            "    jal  ra, rt_putc\n" +
            "    li   a0, 102\n" +
            "    jal  ra, rt_putc\n" +
            "    j    fg_end\n" +
            "fg_finite:\n" +
            "    fmv.d ft6, ft1\n" +
            "    addi t2, t2, -1023\n" +
            "    li   t3, 30103\n" +
            "    mul  t2, t2, t3\n" +
            "    li   t3, 100000\n" +
            "    div  s7, t2, t3\n" +
            "    bltz s7, fg_neg\n" +
            "    mv   a0, s7\n" +
            "    jal  ra, rt_pow10\n" +
            "    fdiv.d ft1, ft1, fa0\n" +
            "    j    fg_adj\n" +
            "fg_neg:\n" +
            "    neg  a0, s7\n" +
            "    jal  ra, rt_pow10\n" +
            "    fmul.d ft1, ft1, fa0\n" +
            "fg_adj:\n" +
            "    la   t0, rt_c_ten\n" +
            "    fld  ft3, 0(t0)\n" +
            "fg_adj_hi:\n" +
            "    fle.d t1, ft3, ft1\n" +
            "    beqz t1, fg_adj_lo\n" +
            "    fdiv.d ft1, ft1, ft3\n" +
            "    addi s7, s7, 1\n" +
            "    j    fg_adj_hi\n" +
            "fg_adj_lo:\n" +
            "    li   t1, 1\n" +
            "    fcvt.d.w ft4, t1\n" +
            "    flt.d t1, ft1, ft4\n" +
            "    beqz t1, fg_round\n" +
            "    fmul.d ft1, ft1, ft3\n" +
            "    addi s7, s7, -1\n" +
            "    j    fg_adj_lo\n" +
            "fg_round:\n" +
            "    la   t0, rt_c_1e5\n" +
            "    fld  ft5, 0(t0)\n" +
            "    fmul.d ft1, ft1, ft5\n" +
            "    fcvt.w.d t3, ft1, rdn\n" +
            "    fcvt.d.w ft7, t3\n" +
            "    fsub.d ft2, ft1, ft7\n" +
            "    la   t0, rt_c_half\n" +
            "    fld  ft8, 0(t0)\n" +
            "    fsub.d ft2, ft2, ft8\n" +
            "    fabs.d ft2, ft2\n" +
            "    la   t0, rt_c_eps\n" +
            "    fld  ft9, 0(t0)\n" +
            "    flt.d t1, ft2, ft9\n" +
            "    bnez t1, fg_exact\n" +
            "    fcvt.w.d t3, ft1\n" +
            "    j    fg_rchk\n" +
            "fg_exact:\n" +
            "    fadd.d ft7, ft7, ft8\n" +
            "    fcvt.d.w ft4, zero\n" +
            "    li   t1, 5\n" +
            "    sub  t2, t1, s7\n" +
            "    bltz t2, fg_exb\n" +
            "    li   t1, 22\n" +
            "    bgt  t2, t1, fg_exfall\n" +
            "    mv   a0, t2\n" +
            "    jal  ra, rt_pow10\n" +
            "    fmul.d ft2, ft6, fa0\n" +
            "    fmsub.d ft3, ft6, fa0, ft2\n" +
            "    fsub.d ft2, ft2, ft7\n" +
            "    feq.d t1, ft2, ft4\n" +
            "    beqz t1, fg_decide\n" +
            "    fmv.d ft2, ft3\n" +
            "    j    fg_decide\n" +
            "fg_exb:\n" +
            "    neg  t2, t2\n" +
            "    li   t1, 22\n" +
            "    bgt  t2, t1, fg_exfall\n" +
            "    mv   a0, t2\n" +
            "    jal  ra, rt_pow10\n" +
            "    fmul.d ft2, ft7, fa0\n" +
            "    fmsub.d ft3, ft7, fa0, ft2\n" +
            "    fsub.d ft2, ft6, ft2\n" +
            "    fcvt.d.w ft4, zero\n" +
            "    feq.d t1, ft2, ft4\n" +
            "    beqz t1, fg_decide\n" +
            "    fneg.d ft2, ft3\n" +
            "fg_decide:\n" +
            "    fcvt.d.w ft4, zero\n" +
            "    flt.d t1, ft4, ft2\n" +
            "    bnez t1, fg_up\n" +
            "    flt.d t1, ft2, ft4\n" +
            "    bnez t1, fg_rchk\n" +
            "    andi t1, t3, 1\n" +
            "    beqz t1, fg_rchk\n" +
            "fg_up:\n" +
            "    addi t3, t3, 1\n" +
            "    j    fg_rchk\n" +
            "fg_exfall:\n" +
            "    fcvt.w.d t3, ft1\n" +
            "fg_rchk:\n" +
            "    li   t4, 1000000\n" +
            "    blt  t3, t4, fg_dig\n" +
            "    li   t3, 100000\n" +
            "    addi s7, s7, 1\n" +
            "fg_dig:\n" +
            "    la   t5, rt_digs\n" +
            "    li   t4, 10\n" +
            "    li   t6, 5\n" +
            "fg_dloop:\n" +
            "    remu t1, t3, t4\n" +
            "    addi t1, t1, 48\n" +
            "    add  t2, t5, t6\n" +
            "    sb   t1, 0(t2)\n" +
            "    divu t3, t3, t4\n" +
            "    addi t6, t6, -1\n" +
            "    bgez t6, fg_dloop\n" +
            "    li   s9, 6\n" +
            "fg_trim:\n" +
            "    li   t1, 1\n" +
            "    ble  s9, t1, fg_style\n" +
            "    add  t2, t5, s9\n" +
            "    lb   t1, -1(t2)\n" +
            "    li   t3, 48\n" +
            "    bne  t1, t3, fg_style\n" +
            "    addi s9, s9, -1\n" +
            "    j    fg_trim\n" +
            "fg_style:\n" +
            "    li   t1, -4\n" +
            "    blt  s7, t1, fg_sci\n" +
            "    li   t1, 6\n" +
            "    bge  s7, t1, fg_sci\n" +
            "    bltz s7, fg_fixneg\n" +
            "    li   a2, 0\n" +
            "fg_ip:\n" +
            "    blt  a2, s9, fg_ip_d\n" +
            "    li   a0, 48\n" +
            "    j    fg_ip_p\n" +
            "fg_ip_d:\n" +
            "    la   t5, rt_digs\n" +
            "    add  t2, t5, a2\n" +
            "    lbu  a0, 0(t2)\n" +
            "fg_ip_p:\n" +
            "    jal  ra, rt_putc\n" +
            "    addi a2, a2, 1\n" +
            "    ble  a2, s7, fg_ip\n" +
            "    addi t1, s7, 1\n" +
            "    ble  s9, t1, fg_end\n" +
            "    li   a0, 46\n" +
            "    jal  ra, rt_putc\n" +
            "    addi a2, s7, 1\n" +
            "fg_fr:\n" +
            "    la   t5, rt_digs\n" +
            "    add  t2, t5, a2\n" +
            "    lbu  a0, 0(t2)\n" +
            "    jal  ra, rt_putc\n" +
            "    addi a2, a2, 1\n" +
            "    blt  a2, s9, fg_fr\n" +
            "    j    fg_end\n" +
            "fg_fixneg:\n" +
            "    li   a0, 48\n" +
            "    jal  ra, rt_putc\n" +
            "    li   a0, 46\n" +
            "    jal  ra, rt_putc\n" +
            "    neg  a2, s7\n" +
            "    addi a2, a2, -1\n" +
            "fg_zl:\n" +
            "    blez a2, fg_zd\n" +
            "    li   a0, 48\n" +
            "    jal  ra, rt_putc\n" +
            "    addi a2, a2, -1\n" +
            "    j    fg_zl\n" +
            "fg_zd:\n" +
            "    li   a2, 0\n" +
            "fg_nd:\n" +
            "    la   t5, rt_digs\n" +
            "    add  t2, t5, a2\n" +
            "    lbu  a0, 0(t2)\n" +
            "    jal  ra, rt_putc\n" +
            "    addi a2, a2, 1\n" +
            "    blt  a2, s9, fg_nd\n" +
            "    j    fg_end\n" +
            "fg_sci:\n" +
            "    la   t5, rt_digs\n" +
            "    lbu  a0, 0(t5)\n" +
            "    jal  ra, rt_putc\n" +
            "    li   t1, 1\n" +
            "    ble  s9, t1, fg_sci_e\n" +
            "    li   a0, 46\n" +
            "    jal  ra, rt_putc\n" +
            "    li   a2, 1\n" +
            "fg_sd:\n" +
            "    la   t5, rt_digs\n" +
            "    add  t2, t5, a2\n" +
            "    lbu  a0, 0(t2)\n" +
            "    jal  ra, rt_putc\n" +
            "    addi a2, a2, 1\n" +
            "    blt  a2, s9, fg_sd\n" +
            "fg_sci_e:\n" +
            "    li   a0, 101\n" +
            "    jal  ra, rt_putc\n" +
            "    bltz s7, fg_en\n" +
            "    li   a0, 43\n" +
            "    jal  ra, rt_putc\n" +
            "    mv   a2, s7\n" +
            "    j    fg_ev\n" +
            "fg_en:\n" +
            "    li   a0, 45\n" +
            "    jal  ra, rt_putc\n" +
            "    neg  a2, s7\n" +
            "fg_ev:\n" +
            "    li   t1, 100\n" +
            "    blt  a2, t1, fg_e2d\n" +
            "    div  t2, a2, t1\n" +
            "    addi a0, t2, 48\n" +
            "    jal  ra, rt_putc\n" +
            "    li   t1, 100\n" +
            "    rem  a2, a2, t1\n" +
            "fg_e2d:\n" +
            "    li   t1, 10\n" +
            "    div  t2, a2, t1\n" +
            "    addi a0, t2, 48\n" +
            "    jal  ra, rt_putc\n" +
            "    li   t1, 10\n" +
            "    rem  t2, a2, t1\n" +
            "    addi a0, t2, 48\n" +
            "    jal  ra, rt_putc\n" +
            "fg_end:\n" +
            "    sb   zero, 0(s6)\n" +
            "    sub  a0, s6, s8\n" +
            "    lw   ra, 28(sp)\n" +
            "    lw   s6, 24(sp)\n" +
            "    lw   s7, 20(sp)\n" +
            "    lw   s8, 16(sp)\n" +
            "    lw   s9, 12(sp)\n" +
            "    addi sp, sp, 32\n" +
            "    ret\n" +
            "rt_getc:\n" +
            "    la   t0, rt_inpos\n" +
            "    lw   t1, 0(t0)\n" +
            "    la   t2, rt_inlen\n" +
            "    lw   t3, 0(t2)\n" +
            "    blt  t1, t3, gc_have\n" +
            "    la   t4, rt_ineof\n" +
            "    lw   t5, 0(t4)\n" +
            "    bnez t5, gc_eof\n" +
            "    la   a0, rt_inbuf\n" +
            "    li   a1, 1024\n" +
            "    li   a7, 8\n" +
            "    ecall\n" +
            "    la   t3, rt_inbuf\n" +
            "    mv   t4, t3\n" +
            "gc_len:\n" +
            "    lbu  t5, 0(t4)\n" +
            "    beqz t5, gc_lend\n" +
            "    addi t4, t4, 1\n" +
            "    j    gc_len\n" +
            "gc_lend:\n" +
            "    sub  t3, t4, t3\n" +
            "    beqz t3, gc_seteof\n" +
            "    la   t2, rt_inlen\n" +
            "    sw   t3, 0(t2)\n" +
            "    la   t0, rt_inpos\n" +
            "    sw   zero, 0(t0)\n" +
            "    la   t6, rt_inbuf\n" +
            "    lbu  t5, 0(t6)\n" +
            "    li   t6, 4\n" +
            "    beq  t5, t6, gc_seteof\n" +
            "    li   t6, 10\n" +
            "    bne  t5, t6, gc_nonblank\n" +
            "    li   t6, 1\n" +
            "    bne  t3, t6, gc_nonblank\n" +
            "    la   t6, rt_blanks\n" +
            "    lw   t4, 0(t6)\n" +
            "    addi t4, t4, 1\n" +
            "    sw   t4, 0(t6)\n" +
            "    li   t5, 64\n" +
            "    bge  t4, t5, gc_seteof\n" +
            "    j    rt_getc\n" +
            "gc_nonblank:\n" +
            "    la   t6, rt_blanks\n" +
            "    sw   zero, 0(t6)\n" +
            "    j    rt_getc\n" +
            "gc_seteof:\n" +
            "    la   t4, rt_ineof\n" +
            "    li   t5, 1\n" +
            "    sw   t5, 0(t4)\n" +
            "    la   t2, rt_inlen\n" +
            "    sw   zero, 0(t2)\n" +
            "gc_eof:\n" +
            "    li   a0, -1\n" +
            "    ret\n" +
            "gc_have:\n" +
            "    la   t4, rt_inbuf\n" +
            "    add  t4, t4, t1\n" +
            "    lbu  a0, 0(t4)\n" +
            "    addi t1, t1, 1\n" +
            "    sw   t1, 0(t0)\n" +
            "    ret\n" +
            "rt_ungetc:\n" +
            "    la   t0, rt_inpos\n" +
            "    lw   t1, 0(t0)\n" +
            "    addi t1, t1, -1\n" +
            "    sw   t1, 0(t0)\n" +
            "    ret\n" +
            "rt_skipws:\n" +
            "    addi sp, sp, -16\n" +
            "    sw   ra, 12(sp)\n" +
            "rt_sw_l:\n" +
            "    jal  ra, rt_getc\n" +
            "    bltz a0, rt_sw_e\n" +
            "    li   t0, 32\n" +
            "    ble  a0, t0, rt_sw_l\n" +
            "    jal  ra, rt_ungetc\n" +
            "rt_sw_e:\n" +
            "    lw   ra, 12(sp)\n" +
            "    addi sp, sp, 16\n" +
            "    ret\n" +
            "rt_read_char:\n" +
            "    addi sp, sp, -16\n" +
            "    sw   ra, 12(sp)\n" +
            "    jal  ra, rt_skipws\n" +
            "    bltz a0, rt_rc_z\n" +
            "    jal  ra, rt_getc\n" +
            "    j    rt_rc_e\n" +
            "rt_rc_z:\n" +
            "    li   a0, 0\n" +
            "rt_rc_e:\n" +
            "    lw   ra, 12(sp)\n" +
            "    addi sp, sp, 16\n" +
            "    ret\n" +
            "rt_read_int:\n" +
            "    addi sp, sp, -32\n" +
            "    sw   ra, 28(sp)\n" +
            "    sw   s6, 24(sp)\n" +
            "    sw   s7, 20(sp)\n" +
            "    jal  ra, rt_skipws\n" +
            "    bltz a0, rt_ri_z\n" +
            "    li   s6, 0\n" +
            "    li   t0, 45\n" +
            "    beq  a0, t0, rt_ri_n\n" +
            "    li   t0, 43\n" +
            "    beq  a0, t0, rt_ri_p\n" +
            "    j    rt_ri_d\n" +
            "rt_ri_n:\n" +
            "    li   s6, 1\n" +
            "rt_ri_p:\n" +
            "    jal  ra, rt_getc\n" +
            "rt_ri_d:\n" +
            "    li   s7, 0\n" +
            "rt_ri_l:\n" +
            "    jal  ra, rt_getc\n" +
            "    bltz a0, rt_ri_e\n" +
            "    addi t0, a0, -48\n" +
            "    li   t1, 9\n" +
            "    bltu t1, t0, rt_ri_u\n" +
            "    li   t2, 10\n" +
            "    mul  s7, s7, t2\n" +
            "    add  s7, s7, t0\n" +
            "    j    rt_ri_l\n" +
            "rt_ri_u:\n" +
            "    jal  ra, rt_ungetc\n" +
            "rt_ri_e:\n" +
            "    mv   a0, s7\n" +
            "    beqz s6, rt_ri_r\n" +
            "    neg  a0, a0\n" +
            "    j    rt_ri_r\n" +
            "rt_ri_z:\n" +
            "    li   a0, 0\n" +
            "rt_ri_r:\n" +
            "    lw   ra, 28(sp)\n" +
            "    lw   s6, 24(sp)\n" +
            "    lw   s7, 20(sp)\n" +
            "    addi sp, sp, 32\n" +
            "    ret\n" +
            "rt_read_str:\n" +
            "    addi sp, sp, -16\n" +
            "    sw   ra, 12(sp)\n" +
            "    sw   s6, 8(sp)\n" +
            "    mv   s6, a0\n" +
            "    jal  ra, rt_skipws\n" +
            "    bltz a0, rt_rs_e\n" +
            "rt_rs_l:\n" +
            "    jal  ra, rt_getc\n" +
            "    bltz a0, rt_rs_e\n" +
            "    li   t0, 32\n" +
            "    ble  a0, t0, rt_rs_u\n" +
            "    sb   a0, 0(s6)\n" +
            "    addi s6, s6, 1\n" +
            "    j    rt_rs_l\n" +
            "rt_rs_u:\n" +
            "    jal  ra, rt_ungetc\n" +
            "rt_rs_e:\n" +
            "    sb   zero, 0(s6)\n" +
            "    lw   ra, 12(sp)\n" +
            "    lw   s6, 8(sp)\n" +
            "    addi sp, sp, 16\n" +
            "    ret\n" +
            "rt_read_double:\n" +
            "    addi sp, sp, -48\n" +
            "    sw   ra, 44(sp)\n" +
            "    sw   s6, 40(sp)\n" +
            "    sw   s7, 36(sp)\n" +
            "    sw   s8, 32(sp)\n" +
            "    sw   s9, 28(sp)\n" +
            "    sw   s10, 24(sp)\n" +
            "    li   s6, 0\n" +
            "    li   s7, 0\n" +
            "    li   s8, 0\n" +
            "    li   s9, 0\n" +
            "    li   s10, 0\n" +
            "    fcvt.d.w ft8, zero\n" +
            "    la   t0, rt_c_ten\n" +
            "    fld  ft9, 0(t0)\n" +
            "    jal  ra, rt_skipws\n" +
            "    bltz a0, rd_zero\n" +
            "    li   t0, 45\n" +
            "    beq  a0, t0, rd_n\n" +
            "    li   t0, 43\n" +
            "    beq  a0, t0, rd_p\n" +
            "    j    rd_ip\n" +
            "rd_n:\n" +
            "    li   s6, 1\n" +
            "rd_p:\n" +
            "    jal  ra, rt_getc\n" +
            "rd_ip:\n" +
            "    jal  ra, rt_getc\n" +
            "    bltz a0, rd_fin\n" +
            "    addi t0, a0, -48\n" +
            "    li   t1, 9\n" +
            "    bltu t1, t0, rd_ip_end\n" +
            "    fmul.d ft8, ft8, ft9\n" +
            "    fcvt.d.w ft10, t0\n" +
            "    fadd.d ft8, ft8, ft10\n" +
            "    addi s9, s9, 1\n" +
            "    j    rd_ip\n" +
            "rd_ip_end:\n" +
            "    li   t1, 46\n" +
            "    bne  a0, t1, rd_chk\n" +
            "rd_fp:\n" +
            "    jal  ra, rt_getc\n" +
            "    bltz a0, rd_fin\n" +
            "    addi t0, a0, -48\n" +
            "    li   t1, 9\n" +
            "    bltu t1, t0, rd_chk\n" +
            "    fmul.d ft8, ft8, ft9\n" +
            "    fcvt.d.w ft10, t0\n" +
            "    fadd.d ft8, ft8, ft10\n" +
            "    addi s7, s7, 1\n" +
            "    addi s9, s9, 1\n" +
            "    j    rd_fp\n" +
            "rd_chk:\n" +
            "    li   t1, 101\n" +
            "    beq  a0, t1, rd_exp\n" +
            "    li   t1, 69\n" +
            "    beq  a0, t1, rd_exp\n" +
            "    jal  ra, rt_ungetc\n" +
            "    j    rd_fin\n" +
            "rd_exp:\n" +
            "    jal  ra, rt_getc\n" +
            "    bltz a0, rd_apply\n" +
            "    li   t1, 45\n" +
            "    beq  a0, t1, rd_en\n" +
            "    li   t1, 43\n" +
            "    beq  a0, t1, rd_ep\n" +
            "    j    rd_ed\n" +
            "rd_en:\n" +
            "    li   s10, 1\n" +
            "rd_ep:\n" +
            "    jal  ra, rt_getc\n" +
            "    bltz a0, rd_apply\n" +
            "rd_ed:\n" +
            "    addi t0, a0, -48\n" +
            "    li   t1, 9\n" +
            "    bltu t1, t0, rd_ed_u\n" +
            "    li   t2, 10\n" +
            "    mul  s8, s8, t2\n" +
            "    add  s8, s8, t0\n" +
            "    jal  ra, rt_getc\n" +
            "    bltz a0, rd_apply\n" +
            "    j    rd_ed\n" +
            "rd_ed_u:\n" +
            "    jal  ra, rt_ungetc\n" +
            "rd_apply:\n" +
            "    beqz s10, rd_fin\n" +
            "    neg  s8, s8\n" +
            "rd_fin:\n" +
            "    beqz s9, rd_zero\n" +
            "    sub  s8, s8, s7\n" +
            "rd_bh:\n" +
            "    li   t1, 280\n" +
            "    ble  s8, t1, rd_bl\n" +
            "    li   a0, 280\n" +
            "    jal  ra, rt_pow10\n" +
            "    fmul.d ft8, ft8, fa0\n" +
            "    addi s8, s8, -280\n" +
            "    j    rd_bh\n" +
            "rd_bl:\n" +
            "    li   t1, -280\n" +
            "    bge  s8, t1, rd_mp\n" +
            "    li   a0, 280\n" +
            "    jal  ra, rt_pow10\n" +
            "    fdiv.d ft8, ft8, fa0\n" +
            "    addi s8, s8, 280\n" +
            "    j    rd_bl\n" +
            "rd_mp:\n" +
            "    bgez s8, rd_mul\n" +
            "    neg  a0, s8\n" +
            "    jal  ra, rt_pow10\n" +
            "    fdiv.d fa0, ft8, fa0\n" +
            "    j    rd_sign\n" +
            "rd_mul:\n" +
            "    mv   a0, s8\n" +
            "    jal  ra, rt_pow10\n" +
            "    fmul.d fa0, ft8, fa0\n" +
            "rd_sign:\n" +
            "    beqz s6, rd_ret\n" +
            "    fneg.d fa0, fa0\n" +
            "    j    rd_ret\n" +
            "rd_zero:\n" +
            "    fcvt.d.w fa0, zero\n" +
            "rd_ret:\n" +
            "    lw   ra, 44(sp)\n" +
            "    lw   s6, 40(sp)\n" +
            "    lw   s7, 36(sp)\n" +
            "    lw   s8, 32(sp)\n" +
            "    lw   s9, 28(sp)\n" +
            "    lw   s10, 24(sp)\n" +
            "    addi sp, sp, 48\n" +
            "    ret\n";

    private RuntimeRiscV() {}

    public static String texto() {
        return RUNTIME;
    }
}

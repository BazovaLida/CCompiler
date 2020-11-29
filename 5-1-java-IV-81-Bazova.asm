.386
.model flat, stdcall
option casemap :none

include C:\masm32\include\kernel32.inc
include C:\masm32\include\user32.inc

includelib C:\masm32\lib\kernel32.lib
includelib C:\masm32\lib\user32.lib

main PROTO

.data
msg_title db "Result", 0
buffer db 128 dup(?)
format db "%d",0

.code
start:
	invoke main
	invoke wsprintf, addr buffer, addr format, eax
	invoke MessageBox, 0, addr buffer, addr msg_title, 0
	invoke ExitProcess, 0

main proc
	push ebp
	mov ebp, esp
	push 10
	pop [ebp-8];	k_var
	push [ebp-8]     ;k_val
pop eax	;if
cmp eax, 0
je _L2

	push [ebp-8]     ;k_val
	pop [ebp-12];	c_var
	push 0
	pop [ebp-12];	i_var
_for_start1:
	push 9
	pop eax; expression
	pop ebx; value
	cmp ebx, eax
	jle _for_start12;if value less than expr

	push [ebp-12]     ;i_val
	push 1
	pop [ebp-12];	i_var
	push 10
	pop [ebp-16];	c_var
	push [ebp-8]     ;k_val
	pop eax ;here is the result
	mov esp, ebp  ; restore ESP; now it points to old EBP
	pop ebp       ; restore old EBP; now ESP is where it was before prologue
	ret 0

main ENDP
END start
